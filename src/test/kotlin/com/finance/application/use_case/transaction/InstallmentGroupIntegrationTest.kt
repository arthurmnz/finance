package com.finance.application.use_case.transaction

import com.finance.application.dto.transaction.CreateTransactionRequest
import com.finance.application.dto.transaction.InstallmentGroupItemRequest
import com.finance.application.dto.transaction.UpdateInstallmentGroupAmountsRequest
import com.finance.application.dto.transaction.UpdateTransactionRequest
import com.finance.application.service.CreditCardService
import com.finance.domain.entity.AccountEntity
import com.finance.domain.entity.CreditCardEntity
import com.finance.domain.entity.GroupEntity
import com.finance.domain.entity.GroupMemberEntity
import com.finance.domain.entity.UserEntity
import com.finance.domain.enum.AccountType
import com.finance.domain.enum.GroupRole
import com.finance.domain.enum.InstallmentRemainderStrategy
import com.finance.domain.enum.RecurrenceFrequency
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.CreditCardRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.repository.UserRepository
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@Transactional
class InstallmentGroupIntegrationTest @Autowired constructor(
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val creditCardRepository: CreditCardRepository,
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val transactionRepository: TransactionRepository,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val listInstallmentGroupUseCase: ListInstallmentGroupUseCase,
    private val updateInstallmentGroupAmountsUseCase: UpdateInstallmentGroupAmountsUseCase,
    private val creditCardService: CreditCardService
) {

    private fun createUser(prefix: String): UserEntity =
        userRepository.save(
            UserEntity(
                firstName = Name(prefix),
                lastName = Name("Test"),
                email = Email("$prefix.${UUID.randomUUID()}@finance.com"),
                passwordHash = PasswordHash("hash123")
            )
        )

    @Test
    fun `should persist 19994 in 4x, calculate CreditCardService limit and invoices, and support group listing and batch edit`() {
        val owner = createUser("Owner")
        val member = createUser("Member")
        val group = groupRepository.save(GroupEntity(name = "Casa"))
        groupMemberRepository.save(GroupMemberEntity(groupId = group.id, userId = owner.id, role = GroupRole.OWNER))
        groupMemberRepository.save(GroupMemberEntity(groupId = group.id, userId = member.id, role = GroupRole.MEMBER))

        val account = accountRepository.save(
            AccountEntity(
                ownerId = owner.id,
                name = "Conta Principal",
                startBalance = 500000,
                type = AccountType.CHECKING,
                tax = null
            )
        )
        val card = creditCardRepository.save(
            CreditCardEntity(
                ownerId = owner.id,
                accountId = account.id,
                name = "Roxinho",
                limitAmount = 500000,
                closingDay = 10,
                dueDay = 17
            ).also { it.linkToGroup(group.id) }
        )

        val created = createTransactionUseCase.execute(
            member.id,
            CreateTransactionRequest(
                title = "Cafeteira",
                amount = 19994,
                date = LocalDate.of(2026, 11, 5),
                accountId = account.id,
                type = TransactionType.EXPENSE,
                creditCardId = card.id,
                isRecurring = true,
                recurrenceFrequency = RecurrenceFrequency.MONTHLY,
                recurrenceInterval = 1,
                recurrenceInstallments = 4,
                isInfinite = false,
                installmentAmountMode = "TOTAL",
                remainderStrategy = InstallmentRemainderStrategy.FIRST_INSTALLMENT
            )
        )

        assertEquals(listOf(5000, 4998, 4998, 4998), created.map { it.amount })
        val recurrenceGroupId = created[0].recurrenceGroupId
        assertNotNull(recurrenceGroupId)

        // CreditCardService deve refletir usedLimit = 19994 e faturas 5000, 4998, 4998, 4998
        val cardSummary = creditCardService.listAccessibleCards(owner.id).first { it.id == card.id }
        assertEquals(19994, cardSummary.usedLimit)
        assertEquals(5000, creditCardService.getInvoice(card.id, 11, 2026, owner.id).purchasesAmount)
        assertEquals(4998, creditCardService.getInvoice(card.id, 12, 2026, owner.id).purchasesAmount)
        assertEquals(4998, creditCardService.getInvoice(card.id, 1, 2027, owner.id).purchasesAmount)
        assertEquals(4998, creditCardService.getInvoice(card.id, 2, 2027, owner.id).purchasesAmount)

        // Edição da 1ª parcela com updateAllFuture = true e updateFutureAmounts = false preserva 4998 nas parcelas 2..4
        updateTransactionUseCase.execute(
            member.id,
            created[0].id,
            UpdateTransactionRequest(
                title = "Cafeteira Espresso",
                amount = 5000,
                date = LocalDate.of(2026, 11, 5),
                status = TransactionStatus.COMPLETED,
                updateAllFuture = true,
                updateFutureAmounts = false
            )
        )
        val listed = listInstallmentGroupUseCase.execute(recurrenceGroupId, owner.id)
        assertEquals(listOf(5000, 4998, 4998, 4998), listed.map { it.amount })
        assertEquals(listOf("Cafeteira Espresso", "Cafeteira Espresso", "Cafeteira Espresso", "Cafeteira Espresso"), listed.map { it.title })

        // Edição em lote do grupo pelo autor
        val batchUpdated = updateInstallmentGroupAmountsUseCase.execute(
            member.id,
            recurrenceGroupId,
            UpdateInstallmentGroupAmountsRequest(
                items = listOf(
                    InstallmentGroupItemRequest(created[0].id, 6000),
                    InstallmentGroupItemRequest(created[1].id, 5000),
                    InstallmentGroupItemRequest(created[2].id, 4994),
                    InstallmentGroupItemRequest(created[3].id, 4000)
                )
            )
        )
        assertEquals(listOf(6000, 5000, 4994, 4000), batchUpdated.map { it.amount })
        val updatedCardSummary = creditCardService.listAccessibleCards(owner.id).first { it.id == card.id }
        assertEquals(20000 - 6, updatedCardSummary.usedLimit) // 6000 + 5000 + 4994 + 4000 = 19994

        // Ex-membro removido do grupo perde acesso de leitura e mutação do grupo
        groupMemberRepository.deleteByGroupIdAndUserId(group.id, member.id)
        assertThrows<AccessDeniedException> {
            listInstallmentGroupUseCase.execute(recurrenceGroupId, member.id)
        }
        assertThrows<AccessDeniedException> {
            updateInstallmentGroupAmountsUseCase.execute(
                member.id,
                recurrenceGroupId,
                UpdateInstallmentGroupAmountsRequest(listOf(InstallmentGroupItemRequest(created[0].id, 5500)))
            )
        }
    }

    @Test
    fun `should find future weekly card installments sharing same invoiceOpeningDate and recalculate COMPLETED bank balances on batch edit`() {
        val user = createUser("User")
        val account = accountRepository.save(
            AccountEntity(
                ownerId = user.id,
                name = "Conta Corrente",
                startBalance = 100000,
                type = AccountType.CHECKING,
                tax = null
            )
        )
        val card = creditCardRepository.save(
            CreditCardEntity(
                ownerId = user.id,
                accountId = account.id,
                name = "Card Weekly",
                limitAmount = 500000,
                closingDay = 10,
                dueDay = 17
            )
        )

        // Compra semanal em 01/01/2026 (antes do fechamento dia 10): parcelas 3 e 4 cruzam para fatura 02/2026 com a mesma invoiceOpeningDate (2026-01-10)
        val weekly = createTransactionUseCase.execute(
            user.id,
            CreateTransactionRequest(
                title = "Curso Semanal",
                amount = 10002,
                date = LocalDate.of(2026, 1, 1),
                accountId = account.id,
                type = TransactionType.EXPENSE,
                creditCardId = card.id,
                isRecurring = true,
                recurrenceFrequency = RecurrenceFrequency.WEEKLY,
                recurrenceInterval = 1,
                recurrenceInstallments = 4,
                isInfinite = false,
                installmentAmountMode = "TOTAL",
                remainderStrategy = InstallmentRemainderStrategy.FIRST_INSTALLMENT
            )
        )
        // Parcelas 3 e 4 caem na fatura 02/2026 com a mesma data de abertura (2026-01-10)
        assertEquals(LocalDate.of(2026, 1, 8), weekly[1].date)
        assertEquals(LocalDate.of(2026, 1, 10), weekly[2].date)
        assertEquals(LocalDate.of(2026, 1, 10), weekly[3].date)

        val groupId = weekly[0].recurrenceGroupId!!
        val futuresAfterP3 = transactionRepository.findFutureTransactionsInGroup(
            groupId = groupId,
            date = weekly[2].date,
            installmentNumber = weekly[2].installmentNumber
        )
        // Sem o filtro por installmentNumber, t.date > 2026-01-10 retornaria 0; com installmentNumber > 3 retorna a parcela 4!
        assertEquals(listOf(4), futuresAfterP3.mapNotNull { it.installmentNumber })

        // Parcelamento bancário com parcela 1 COMPLETED (status inicial) e recálculo de saldo no batch edit
        val bankInstallments = createTransactionUseCase.execute(
            user.id,
            CreateTransactionRequest(
                title = "Seguro",
                amount = 30000,
                date = LocalDate.of(2026, 11, 10),
                accountId = account.id,
                type = TransactionType.EXPENSE,
                status = TransactionStatus.COMPLETED,
                isRecurring = true,
                recurrenceFrequency = RecurrenceFrequency.MONTHLY,
                recurrenceInstallments = 3,
                installmentAmountMode = "TOTAL"
            )
        )
        // Saldo inicial 100000 - 10000 (parcela 1 COMPLETED) = 90000
        assertEquals(90000, accountRepository.findById(account.id)!!.balance)

        val bankGroupId = bankInstallments[0].recurrenceGroupId!!
        updateInstallmentGroupAmountsUseCase.execute(
            user.id,
            bankGroupId,
            UpdateInstallmentGroupAmountsRequest(
                items = listOf(
                    InstallmentGroupItemRequest(bankInstallments[0].id, 15000), // COMPLETED: +10000 -15000 => saldo 85000
                    InstallmentGroupItemRequest(bankInstallments[1].id, 7500),  // PENDING: não mexe no saldo
                    InstallmentGroupItemRequest(bankInstallments[2].id, 7500)   // PENDING: não mexe no saldo
                )
            )
        )
        assertEquals(85000, accountRepository.findById(account.id)!!.balance)
    }
}
