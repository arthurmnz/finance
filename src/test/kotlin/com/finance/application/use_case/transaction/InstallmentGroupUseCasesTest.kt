package com.finance.application.use_case.transaction

import com.finance.application.dto.transaction.InstallmentGroupItemRequest
import com.finance.application.dto.transaction.UpdateInstallmentGroupAmountsRequest
import com.finance.application.service.AccountAccessService
import com.finance.domain.entity.AccountEntity
import com.finance.domain.entity.CreditCardEntity
import com.finance.domain.entity.CreditCardInvoiceEntity
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.entity.UserEntity
import com.finance.domain.enum.AccountType
import com.finance.domain.enum.RecurrenceFrequency
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.InvalidTransactionException
import com.finance.domain.exception.TransactionNotFoundException
import com.finance.domain.exception.UnauthorizedException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.CreditCardInvoiceRepository
import com.finance.domain.repository.CreditCardRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.JointAccountMemberRepository
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.repository.UserRepository
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

class InstallmentGroupUseCasesTest {

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(): T {
        Mockito.any<T>()
        return null as T
    }

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var creditCardRepository: CreditCardRepository
    private lateinit var creditCardInvoiceRepository: CreditCardInvoiceRepository
    private lateinit var groupMemberRepository: GroupMemberRepository
    private lateinit var jointAccountMemberRepository: JointAccountMemberRepository
    private lateinit var userRepository: UserRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var accountAccessService: AccountAccessService

    private lateinit var listUseCase: ListInstallmentGroupUseCase
    private lateinit var updateAmountsUseCase: UpdateInstallmentGroupAmountsUseCase

    @BeforeEach
    fun setup() {
        transactionRepository = mock(TransactionRepository::class.java)
        accountRepository = mock(AccountRepository::class.java)
        creditCardRepository = mock(CreditCardRepository::class.java)
        creditCardInvoiceRepository = mock(CreditCardInvoiceRepository::class.java)
        groupMemberRepository = mock(GroupMemberRepository::class.java)
        jointAccountMemberRepository = mock(JointAccountMemberRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        categoryRepository = mock(CategoryRepository::class.java)
        accountAccessService = AccountAccessService(groupMemberRepository, jointAccountMemberRepository)

        listUseCase = ListInstallmentGroupUseCase(
            transactionRepository = transactionRepository,
            accountRepository = accountRepository,
            creditCardRepository = creditCardRepository,
            accountAccessService = accountAccessService,
            userRepository = userRepository,
            categoryRepository = categoryRepository
        )
        updateAmountsUseCase = UpdateInstallmentGroupAmountsUseCase(
            transactionRepository = transactionRepository,
            accountRepository = accountRepository,
            creditCardRepository = creditCardRepository,
            creditCardInvoiceRepository = creditCardInvoiceRepository,
            accountAccessService = accountAccessService,
            userRepository = userRepository,
            categoryRepository = categoryRepository
        )
    }

    @Test
    fun `should list installment group ordered by installmentNumber and date and return 404 when empty`() {
        val user = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val groupId = UUID.randomUUID()
        val card = CreditCardEntity(
            id = UUID.randomUUID(),
            ownerId = user.id,
            accountId = UUID.randomUUID(),
            name = "Nubank",
            limitAmount = 500000,
            closingDay = 10,
            dueDay = 17
        )

        val p2 = TransactionEntity(
            title = "Compra",
            amount = 4998,
            date = LocalDate.of(2026, 1, 10),
            responsibleId = user.id,
            accountId = card.accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            recurrenceGroupId = groupId,
            recurrenceFrequency = RecurrenceFrequency.MONTHLY,
            recurrenceInterval = 1,
            creditCardId = card.id,
            invoiceMonth = 2,
            invoiceYear = 2026,
            installmentNumber = 2,
            totalInstallments = 3
        )
        val p1 = TransactionEntity(
            title = "Compra",
            amount = 5000,
            date = LocalDate.of(2025, 12, 20),
            responsibleId = user.id,
            accountId = card.accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            recurrenceGroupId = groupId,
            recurrenceFrequency = RecurrenceFrequency.MONTHLY,
            recurrenceInterval = 1,
            creditCardId = card.id,
            invoiceMonth = 1,
            invoiceYear = 2026,
            installmentNumber = 1,
            totalInstallments = 3
        )
        val p3 = TransactionEntity(
            title = "Compra",
            amount = 4998,
            date = LocalDate.of(2026, 2, 10),
            responsibleId = user.id,
            accountId = card.accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            recurrenceGroupId = groupId,
            recurrenceFrequency = RecurrenceFrequency.MONTHLY,
            recurrenceInterval = 1,
            creditCardId = card.id,
            invoiceMonth = 3,
            invoiceYear = 2026,
            installmentNumber = 3,
            totalInstallments = 3
        )

        `when`(transactionRepository.findByRecurrenceGroupIdOrderByInstallmentNumberAscDateAsc(groupId))
            .thenReturn(listOf(p2, p3, p1))
        `when`(creditCardRepository.findById(card.id)).thenReturn(card)
        `when`(userRepository.findAll()).thenReturn(listOf(user))

        val result = listUseCase.execute(groupId, user.id)
        assertEquals(listOf(1, 2, 3), result.map { it.installmentNumber })
        assertEquals(listOf(5000, 4998, 4998), result.map { it.amount })

        val missingGroupId = UUID.randomUUID()
        `when`(transactionRepository.findByRecurrenceGroupIdOrderByInstallmentNumberAscDateAsc(missingGroupId))
            .thenReturn(emptyList())
        assertThrows<TransactionNotFoundException> {
            listUseCase.execute(missingGroupId, user.id)
        }
    }

    @Test
    fun `should enforce IDOR and ex-member checks on every transaction in ListInstallmentGroupUseCase`() {
        val owner = UserEntity(Name("Owner"), Name("User"), Email("owner@finance.com"), PasswordHash("hash"))
        val exMember = UserEntity(Name("Ex"), Name("Member"), Email("ex@finance.com"), PasswordHash("hash"))
        val familyGroupId = UUID.randomUUID()
        val recurrenceGroupId = UUID.randomUUID()

        val card = CreditCardEntity(
            id = UUID.randomUUID(),
            ownerId = owner.id,
            accountId = UUID.randomUUID(),
            name = "Cartão Família",
            limitAmount = 500000,
            closingDay = 10,
            dueDay = 17,
            groupId = familyGroupId
        )
        val accessibleAcc = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = exMember.id,
            name = "Conta Ex",
            startBalance = 50000,
            balance = 50000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )
        val privateOwnerAcc = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = owner.id,
            name = "Conta Secreta Owner",
            startBalance = 50000,
            balance = 50000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )

        val cardTx = TransactionEntity(
            title = "Compra Cartão",
            amount = 5000,
            date = LocalDate.of(2026, 11, 10),
            responsibleId = exMember.id,
            accountId = card.accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            recurrenceGroupId = recurrenceGroupId,
            creditCardId = card.id,
            installmentNumber = 1,
            totalInstallments = 2
        )

        `when`(transactionRepository.findByRecurrenceGroupIdOrderByInstallmentNumberAscDateAsc(recurrenceGroupId))
            .thenReturn(listOf(cardTx))
        `when`(creditCardRepository.findById(card.id)).thenReturn(card)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(familyGroupId, exMember.id)).thenReturn(false)

        // Ex-member loses read access even though responsibleId == exMember.id
        assertThrows<AccessDeniedException> {
            listUseCase.execute(recurrenceGroupId, exMember.id)
        }

        // Mixed group where 1st tx is accessible but 2nd tx belongs to another user's private account -> blocked
        val bankGroupId = UUID.randomUUID()
        val tx1 = TransactionEntity(
            title = "Parcela 1",
            amount = 1000,
            date = LocalDate.of(2026, 11, 10),
            responsibleId = exMember.id,
            accountId = accessibleAcc.id,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            recurrenceGroupId = bankGroupId,
            installmentNumber = 1,
            totalInstallments = 2
        )
        val tx2 = TransactionEntity(
            title = "Parcela 2",
            amount = 1000,
            date = LocalDate.of(2026, 12, 10),
            responsibleId = owner.id,
            accountId = privateOwnerAcc.id,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.PENDING,
            recurrenceGroupId = bankGroupId,
            installmentNumber = 2,
            totalInstallments = 2
        )
        `when`(transactionRepository.findByRecurrenceGroupIdOrderByInstallmentNumberAscDateAsc(bankGroupId))
            .thenReturn(listOf(tx1, tx2))
        `when`(accountRepository.findById(accessibleAcc.id)).thenReturn(accessibleAcc)
        `when`(accountRepository.findById(privateOwnerAcc.id)).thenReturn(privateOwnerAcc)

        assertThrows<AccessDeniedException> {
            listUseCase.execute(bankGroupId, exMember.id)
        }
    }

    @Test
    fun `should update installment group amounts in batch and recalculate COMPLETED bank account balances`() {
        val user = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val sourceAcc = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = user.id,
            name = "Conta Corrente",
            startBalance = 100000,
            balance = 90000, // 10000 já debitados pela parcela 1 COMPLETED
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )
        val groupId = UUID.randomUUID()

        val tx1 = TransactionEntity(
            title = "Curso",
            amount = 10000,
            date = LocalDate.of(2026, 11, 10),
            responsibleId = user.id,
            accountId = sourceAcc.id,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            recurrenceGroupId = groupId,
            recurrenceFrequency = RecurrenceFrequency.MONTHLY,
            recurrenceInterval = 1,
            installmentNumber = 1,
            totalInstallments = 3
        )
        val tx2 = TransactionEntity(
            title = "Curso",
            amount = 10000,
            date = LocalDate.of(2026, 12, 10),
            responsibleId = user.id,
            accountId = sourceAcc.id,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.PENDING,
            recurrenceGroupId = groupId,
            recurrenceFrequency = RecurrenceFrequency.MONTHLY,
            recurrenceInterval = 1,
            installmentNumber = 2,
            totalInstallments = 3
        )
        val tx3 = TransactionEntity(
            title = "Curso",
            amount = 10000,
            date = LocalDate.of(2027, 1, 10),
            responsibleId = user.id,
            accountId = sourceAcc.id,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED, // também efetivada -> saldo também deve ajustar
            recurrenceGroupId = groupId,
            recurrenceFrequency = RecurrenceFrequency.MONTHLY,
            recurrenceInterval = 1,
            installmentNumber = 3,
            totalInstallments = 3
        )

        `when`(transactionRepository.findByRecurrenceGroupIdOrderByInstallmentNumberAscDateAsc(groupId))
            .thenReturn(listOf(tx1, tx2, tx3))
        `when`(accountRepository.findById(sourceAcc.id)).thenReturn(sourceAcc)
        `when`(userRepository.findAll()).thenReturn(listOf(user))
        `when`(transactionRepository.saveAll(anyNonNull())).thenAnswer { it.getArgument<List<TransactionEntity>>(0) }

        val updated = updateAmountsUseCase.execute(
            user.id,
            groupId,
            UpdateInstallmentGroupAmountsRequest(
                items = listOf(
                    InstallmentGroupItemRequest(tx3.id, 12000),
                    InstallmentGroupItemRequest(tx1.id, 8000),
                    InstallmentGroupItemRequest(tx2.id, 10000)
                )
            )
        )

        assertEquals(listOf(1, 2, 3), updated.map { it.installmentNumber })
        assertEquals(listOf(8000, 10000, 12000), updated.map { it.amount })
        // tx1: +10000 -8000 = +2000; tx3: +10000 -12000 = -2000; tx2 (PENDING): 0 => saldo mantido em 90000
        assertEquals(90000, sourceAcc.balance)

        // Agora altera tx1 (COMPLETED) para 5000 (+5000 no saldo)
        val updated2 = updateAmountsUseCase.execute(
            user.id,
            groupId,
            UpdateInstallmentGroupAmountsRequest(
                items = listOf(InstallmentGroupItemRequest(tx1.id, 5000))
            )
        )
        assertEquals(1, updated2.size)
        assertEquals(5000, updated2[0].amount)
        assertEquals(93000, sourceAcc.balance)
    }

    @Test
    fun `should validate request items, IDOR, and avoid any mutation on failure in UpdateInstallmentGroupAmountsUseCase`() {
        val owner = UserEntity(Name("Owner"), Name("User"), Email("owner@finance.com"), PasswordHash("hash"))
        val attacker = UserEntity(Name("Attacker"), Name("User"), Email("attacker@finance.com"), PasswordHash("hash"))
        val familyGroupId = UUID.randomUUID()
        val groupId = UUID.randomUUID()

        val card = CreditCardEntity(
            id = UUID.randomUUID(),
            ownerId = owner.id,
            accountId = UUID.randomUUID(),
            name = "Cartão Grupo",
            limitAmount = 500000,
            closingDay = 10,
            dueDay = 17,
            groupId = familyGroupId
        )
        val tx1 = TransactionEntity(
            title = "Notebook",
            amount = 5000,
            date = LocalDate.of(2026, 11, 10),
            responsibleId = attacker.id,
            accountId = card.accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            recurrenceGroupId = groupId,
            creditCardId = card.id,
            installmentNumber = 1,
            totalInstallments = 2
        )
        val tx2 = TransactionEntity(
            title = "Notebook",
            amount = 5000,
            date = LocalDate.of(2026, 12, 10),
            responsibleId = owner.id,
            accountId = card.accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            recurrenceGroupId = groupId,
            creditCardId = card.id,
            installmentNumber = 2,
            totalInstallments = 2
        )

        // Lista vazia
        assertThrows<InvalidTransactionException> {
            updateAmountsUseCase.execute(owner.id, groupId, UpdateInstallmentGroupAmountsRequest(emptyList()))
        }
        // IDs duplicados
        assertThrows<InvalidTransactionException> {
            updateAmountsUseCase.execute(
                owner.id,
                groupId,
                UpdateInstallmentGroupAmountsRequest(
                    listOf(
                        InstallmentGroupItemRequest(tx1.id, 4000),
                        InstallmentGroupItemRequest(tx1.id, 6000)
                    )
                )
            )
        }
        // Valor <= 0
        assertThrows<InvalidTransactionException> {
            updateAmountsUseCase.execute(
                owner.id,
                groupId,
                UpdateInstallmentGroupAmountsRequest(listOf(InstallmentGroupItemRequest(tx1.id, 0)))
            )
        }

        // Grupo inexistente -> 404
        `when`(transactionRepository.findByRecurrenceGroupIdOrderByInstallmentNumberAscDateAsc(groupId))
            .thenReturn(emptyList())
        assertThrows<TransactionNotFoundException> {
            updateAmountsUseCase.execute(
                owner.id,
                groupId,
                UpdateInstallmentGroupAmountsRequest(listOf(InstallmentGroupItemRequest(tx1.id, 6000)))
            )
        }

        // Segurança: ex-membro do grupo bloqueado em tx1 mesmo tendo criado tx1
        `when`(transactionRepository.findByRecurrenceGroupIdOrderByInstallmentNumberAscDateAsc(groupId))
            .thenReturn(listOf(tx1, tx2))
        `when`(creditCardRepository.findById(card.id)).thenReturn(card)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(familyGroupId, attacker.id)).thenReturn(false)

        assertThrows<AccessDeniedException> {
            updateAmountsUseCase.execute(
                attacker.id,
                groupId,
                UpdateInstallmentGroupAmountsRequest(listOf(InstallmentGroupItemRequest(tx1.id, 6000)))
            )
        }

        // Mesmo quando attacker ainda está no grupo, tx2 tem responsibleId = owner -> UnauthorizedException antes de qualquer mutação
        `when`(groupMemberRepository.existsByGroupIdAndUserId(familyGroupId, attacker.id)).thenReturn(true)
        assertThrows<UnauthorizedException> {
            updateAmountsUseCase.execute(
                attacker.id,
                groupId,
                UpdateInstallmentGroupAmountsRequest(listOf(InstallmentGroupItemRequest(tx1.id, 6000)))
            )
        }

        // Transação de outro grupo -> InvalidTransactionException
        val ownerOnlyGroupId = UUID.randomUUID()
        val ownerTx = TransactionEntity(
            title = "Compra",
            amount = 5000,
            date = LocalDate.of(2026, 11, 10),
            responsibleId = owner.id,
            accountId = card.accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            recurrenceGroupId = ownerOnlyGroupId,
            creditCardId = card.id,
            installmentNumber = 1,
            totalInstallments = 2
        )
        `when`(transactionRepository.findByRecurrenceGroupIdOrderByInstallmentNumberAscDateAsc(ownerOnlyGroupId))
            .thenReturn(listOf(ownerTx))
        assertThrows<InvalidTransactionException> {
            updateAmountsUseCase.execute(
                owner.id,
                ownerOnlyGroupId,
                UpdateInstallmentGroupAmountsRequest(
                    listOf(
                        InstallmentGroupItemRequest(ownerTx.id, 4000),
                        InstallmentGroupItemRequest(UUID.randomUUID(), 6000)
                    )
                )
            )
        }

        assertEquals(5000, tx1.amount)
        assertEquals(5000, tx2.amount)
        assertEquals(5000, ownerTx.amount)
        verify(transactionRepository, never()).saveAll(anyNonNull())
    }

    @Test
    fun `should reject group batch update when resulting sum overflows Int MAX_VALUE and sync invoice paidAmount for payment tx`() {
        val user = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val card = CreditCardEntity(
            id = UUID.randomUUID(),
            ownerId = user.id,
            accountId = UUID.randomUUID(),
            name = "Roxinho",
            limitAmount = 500000,
            closingDay = 10,
            dueDay = 17
        )
        val groupId = UUID.randomUUID()
        val p1 = TransactionEntity(
            title = "Compra Alta",
            amount = 1_000_000_000,
            date = LocalDate.of(2026, 11, 5),
            responsibleId = user.id,
            accountId = card.accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            recurrenceGroupId = groupId,
            creditCardId = card.id,
            installmentNumber = 1,
            totalInstallments = 2
        )
        val p2 = TransactionEntity(
            title = "Compra Alta",
            amount = 1_000_000_000,
            date = LocalDate.of(2026, 12, 5),
            responsibleId = user.id,
            accountId = card.accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            recurrenceGroupId = groupId,
            creditCardId = card.id,
            installmentNumber = 2,
            totalInstallments = 2
        )

        `when`(transactionRepository.findByRecurrenceGroupIdOrderByInstallmentNumberAscDateAsc(groupId))
            .thenReturn(listOf(p1, p2))
        `when`(creditCardRepository.findById(card.id)).thenReturn(card)

        // Soma projetada = 1_500_000_000 + 1_000_000_000 > Int.MAX_VALUE -> InvalidTransactionException
        assertThrows<InvalidTransactionException> {
            updateAmountsUseCase.execute(
                user.id,
                groupId,
                UpdateInstallmentGroupAmountsRequest(
                    listOf(InstallmentGroupItemRequest(p1.id, 1_500_000_000))
                )
            )
        }
        assertEquals(1_000_000_000, p1.amount)
        verify(transactionRepository, never()).saveAll(anyNonNull())

        // Sincronização de invoice.paidAmount caso o grupo contenha pagamento de fatura COMPLETED
        val acc = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = user.id,
            name = "Conta",
            startBalance = 100000,
            balance = 80000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )
        val invoice = CreditCardInvoiceEntity(
            id = UUID.randomUUID(),
            creditCardId = card.id,
            month = 11,
            year = 2026,
            paidAmount = 20000
        )
        val payGroupId = UUID.randomUUID()
        val payTx = TransactionEntity(
            title = "Pagamento fatura Roxinho (11/2026)",
            amount = 20000,
            date = LocalDate.of(2026, 11, 15),
            responsibleId = user.id,
            accountId = acc.id,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            recurrenceGroupId = payGroupId,
            creditCardInvoiceId = invoice.id,
            installmentNumber = 1,
            totalInstallments = 1
        )
        `when`(transactionRepository.findByRecurrenceGroupIdOrderByInstallmentNumberAscDateAsc(payGroupId))
            .thenReturn(listOf(payTx))
        `when`(accountRepository.findById(acc.id)).thenReturn(acc)
        `when`(creditCardInvoiceRepository.findById(invoice.id)).thenReturn(invoice)
        `when`(userRepository.findAll()).thenReturn(listOf(user))
        `when`(transactionRepository.saveAll(anyNonNull())).thenAnswer { it.getArgument<List<TransactionEntity>>(0) }

        updateAmountsUseCase.execute(
            user.id,
            payGroupId,
            UpdateInstallmentGroupAmountsRequest(listOf(InstallmentGroupItemRequest(payTx.id, 35000)))
        )
        assertEquals(65000, acc.balance)
        assertEquals(35000, invoice.paidAmount)
        verify(creditCardInvoiceRepository).save(invoice)
    }
}
