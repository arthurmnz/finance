package com.finance.application.use_case.group

import com.finance.domain.entity.AccountEntity
import com.finance.domain.entity.CreditCardEntity
import com.finance.domain.entity.DebtSettlementEntity
import com.finance.domain.entity.GroupEntity
import com.finance.domain.entity.GroupMemberEntity
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.entity.UserEntity
import com.finance.domain.enum.AccountType
import com.finance.domain.enum.GroupRole
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.CreditCardRepository
import com.finance.domain.repository.DebtSettlementRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.repository.UserRepository
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CalculateGroupDebtsUseCaseTest {

    private lateinit var groupRepository: GroupRepository
    private lateinit var groupMemberRepository: GroupMemberRepository
    private lateinit var creditCardRepository: CreditCardRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var debtSettlementRepository: DebtSettlementRepository
    private lateinit var userRepository: UserRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var useCase: CalculateGroupDebtsUseCase

    @BeforeEach
    fun setup() {
        groupRepository = mock(GroupRepository::class.java)
        groupMemberRepository = mock(GroupMemberRepository::class.java)
        creditCardRepository = mock(CreditCardRepository::class.java)
        accountRepository = mock(AccountRepository::class.java)
        transactionRepository = mock(TransactionRepository::class.java)
        debtSettlementRepository = mock(DebtSettlementRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        categoryRepository = mock(CategoryRepository::class.java)

        useCase = CalculateGroupDebtsUseCase(
            groupRepository = groupRepository,
            groupMemberRepository = groupMemberRepository,
            creditCardRepository = creditCardRepository,
            accountRepository = accountRepository,
            transactionRepository = transactionRepository,
            debtSettlementRepository = debtSettlementRepository,
            userRepository = userRepository,
            categoryRepository = categoryRepository
        )
    }

    @Test
    fun `should calculate monthly net debts correctly with settlements`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val userB = UserEntity(Name("Maria"), Name("Silva"), Email("maria@finance.com"), PasswordHash("hash"))
        val group = GroupEntity(id = UUID.randomUUID(), name = "Família")

        `when`(groupRepository.findById(group.id)).thenReturn(group)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, userA.id)).thenReturn(true)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, userB.id)).thenReturn(true)
        `when`(userRepository.findAll()).thenReturn(listOf(userA, userB))

        // Cartão pertence a Maria (userB), vinculado ao grupo
        val cardB = CreditCardEntity(
            id = UUID.randomUUID(),
            ownerId = userB.id,
            accountId = UUID.randomUUID(),
            name = "Cartão Maria",
            limitAmount = 500000,
            closingDay = 25,
            dueDay = 5,
            groupId = group.id
        )
        `when`(creditCardRepository.findAllByGroupId(group.id)).thenReturn(listOf(cardB))

        // Arthur fez uma compra de R$ 100,00 no cartão de Maria para Nov/2026
        val txNov = TransactionEntity(
            title = "Supermercado",
            amount = 10000,
            date = LocalDate.of(2026, 11, 10),
            responsibleId = userA.id,
            accountId = cardB.accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            creditCardId = cardB.id,
            invoiceMonth = 11,
            invoiceYear = 2026
        )
        // Arthur fez outra compra de R$ 50,00 para Dez/2026
        val txDez = TransactionEntity(
            title = "Farmácia",
            amount = 5000,
            date = LocalDate.of(2026, 12, 1),
            responsibleId = userA.id,
            accountId = cardB.accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            creditCardId = cardB.id,
            invoiceMonth = 12,
            invoiceYear = 2026
        )

        `when`(transactionRepository.findByCreditCardId(cardB.id)).thenReturn(listOf(txNov, txDez))

        // Arthur já fez um acerto parcial de R$ 30,00 para Nov/2026
        val settlementNov = DebtSettlementEntity(
            id = UUID.randomUUID(),
            groupId = group.id,
            debtorId = userA.id,
            creditorId = userB.id,
            amount = 3000,
            targetMonth = 11,
            targetYear = 2026,
            date = LocalDate.of(2026, 11, 12)
        )
        `when`(debtSettlementRepository.findByGroupId(group.id)).thenReturn(listOf(settlementNov))

        val response = useCase.execute(group.id, userA.id)

        // Verificações
        // Nov/2026: 10000 - 3000 = 7000
        // Dez/2026: 5000 - 0 = 5000
        // Total que Arthur deve: 7000 + 5000 = 12000 (R$ 120,00)
        assertEquals(12000, response.totalOwedByMe)
        assertEquals(0, response.totalReceivableByMe)

        val monthNov = response.monthlyBalances.find { it.month == 11 && it.year == 2026 }
        assertNotNull(monthNov)
        assertEquals(1, monthNov.userBalances.size)
        val balanceNov = monthNov.userBalances[0]
        assertEquals(userA.id, balanceNov.debtor.id)
        assertEquals(userB.id, balanceNov.creditor.id)
        assertEquals(7000, balanceNov.netAmount)

        val monthDez = response.monthlyBalances.find { it.month == 12 && it.year == 2026 }
        assertNotNull(monthDez)
        val balanceDez = monthDez.userBalances[0]
        assertEquals(userA.id, balanceDez.debtor.id)
        assertEquals(userB.id, balanceDez.creditor.id)
        assertEquals(5000, balanceDez.netAmount)
    }

    @Test
    fun `should assign credit card purchase debt to transaction date month instead of invoice month`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val userB = UserEntity(Name("Maria"), Name("Silva"), Email("maria@finance.com"), PasswordHash("hash"))
        val group = GroupEntity(id = UUID.randomUUID(), name = "Família")

        `when`(groupRepository.findById(group.id)).thenReturn(group)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, userA.id)).thenReturn(true)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, userB.id)).thenReturn(true)
        `when`(userRepository.findAll()).thenReturn(listOf(userA, userB))

        val cardA = CreditCardEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            accountId = UUID.randomUUID(),
            name = "Cartão Arthur",
            limitAmount = 500000,
            closingDay = 25,
            dueDay = 5,
            groupId = group.id
        )
        `when`(creditCardRepository.findAllByGroupId(group.id)).thenReturn(listOf(cardA))

        // Usuário B fez compra em outubro (2026-10-28), cuja fatura vence em novembro (invoiceMonth = 11, invoiceYear = 2026)
        val txOct = TransactionEntity(
            title = "Uber",
            amount = 3500,
            date = LocalDate.of(2026, 10, 28),
            responsibleId = userB.id,
            accountId = cardA.accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            creditCardId = cardA.id,
            invoiceMonth = 11,
            invoiceYear = 2026
        )

        `when`(transactionRepository.findByCreditCardId(cardA.id)).thenReturn(listOf(txOct))
        `when`(debtSettlementRepository.findByGroupId(group.id)).thenReturn(emptyList())

        val response = useCase.execute(group.id, userB.id)

        // Arthur tem a receber 3500 de Maria, e Maria deve 3500 a Arthur
        assertEquals(3500, response.totalOwedByMe)
        assertEquals(0, response.totalReceivableByMe)

        // A dívida deve estar no mês de OUTUBRO (mês 10), competência da data da compra, e não de novembro
        val monthOct = response.monthlyBalances.find { it.month == 10 && it.year == 2026 }
        assertNotNull(monthOct)
        val balanceOct = monthOct.userBalances[0]
        assertEquals(userB.id, balanceOct.debtor.id)
        assertEquals(userA.id, balanceOct.creditor.id)
        assertEquals(3500, balanceOct.netAmount)

        // Em novembro não deve haver saldo de dívida
        val monthNov = response.monthlyBalances.find { it.month == 11 && it.year == 2026 }
        assertEquals(0, monthNov?.userBalances?.size ?: 0)
    }

    @Test
    fun `should calculate transfer debts and deduplicate when both accounts belong to the group`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val userB = UserEntity(Name("Maria"), Name("Silva"), Email("maria@finance.com"), PasswordHash("hash"))
        val group = GroupEntity(id = UUID.randomUUID(), name = "Família")

        `when`(groupRepository.findById(group.id)).thenReturn(group)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, userA.id)).thenReturn(true)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, userB.id)).thenReturn(true)
        `when`(userRepository.findAll()).thenReturn(listOf(userA, userB))
        `when`(creditCardRepository.findAllByGroupId(group.id)).thenReturn(emptyList())
        `when`(debtSettlementRepository.findByGroupId(group.id)).thenReturn(emptyList())

        val accA = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Conta Arthur",
            startBalance = 50000,
            balance = 50000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = group.id
        )
        val accB = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userB.id,
            name = "Conta Maria",
            startBalance = 10000,
            balance = 10000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = group.id
        )
        `when`(accountRepository.findAllByGroupId(group.id)).thenReturn(listOf(accA, accB))

        // Transferência de Arthur para Maria gerando dívida (deve aparecer uma única vez mesmo estando em ambas as buscas)
        val debtTransfer = TransactionEntity(
            title = "Empréstimo PIX",
            amount = 15000,
            date = LocalDate.of(2026, 11, 5),
            responsibleId = userA.id,
            accountId = accA.id,
            type = TransactionType.TRANSFER,
            destinationAccountId = accB.id,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            generatesDebt = true
        )

        `when`(transactionRepository.findByAccountId(accA.id)).thenReturn(listOf(debtTransfer))
        `when`(transactionRepository.findByDestinationAccountId(accA.id)).thenReturn(emptyList())
        `when`(transactionRepository.findByAccountId(accB.id)).thenReturn(emptyList())
        `when`(transactionRepository.findByDestinationAccountId(accB.id)).thenReturn(listOf(debtTransfer))

        val response = useCase.execute(group.id, userB.id)

        assertEquals(15000, response.totalOwedByMe)
        assertEquals(0, response.totalReceivableByMe)

        val monthNov = response.monthlyBalances.find { it.month == 11 && it.year == 2026 }
        assertNotNull(monthNov)
        assertEquals(1, monthNov.userBalances.size)
        val balance = monthNov.userBalances[0]
        assertEquals(userB.id, balance.debtor.id)
        assertEquals(userA.id, balance.creditor.id)
        assertEquals(15000, balance.netAmount)
        assertEquals(1, balance.grossDebts[0].transactions.size)
        assertTrue(balance.grossDebts[0].transactions[0].generatesDebt)
    }

    @Test
    fun `should ignore pending transfers and transfers with generatesDebt false`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val userB = UserEntity(Name("Maria"), Name("Silva"), Email("maria@finance.com"), PasswordHash("hash"))
        val group = GroupEntity(id = UUID.randomUUID(), name = "Família")

        `when`(groupRepository.findById(group.id)).thenReturn(group)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, userA.id)).thenReturn(true)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, userB.id)).thenReturn(true)
        `when`(userRepository.findAll()).thenReturn(listOf(userA, userB))
        `when`(creditCardRepository.findAllByGroupId(group.id)).thenReturn(emptyList())
        `when`(debtSettlementRepository.findByGroupId(group.id)).thenReturn(emptyList())

        val accA = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Conta Arthur",
            startBalance = 50000,
            balance = 50000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )
        val accB = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userB.id,
            name = "Conta Maria",
            startBalance = 10000,
            balance = 10000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = group.id
        )
        `when`(accountRepository.findAllByGroupId(group.id)).thenReturn(listOf(accB))
        `when`(accountRepository.findById(accA.id)).thenReturn(accA)

        val pendingDebtTransfer = TransactionEntity(
            title = "PIX agendado",
            amount = 20000,
            date = LocalDate.of(2026, 11, 10),
            responsibleId = userA.id,
            accountId = accA.id,
            type = TransactionType.TRANSFER,
            destinationAccountId = accB.id,
            categoryId = null,
            status = TransactionStatus.PENDING,
            generatesDebt = true
        )
        val nonDebtTransfer = TransactionEntity(
            title = "PIX sem dívida",
            amount = 10000,
            date = LocalDate.of(2026, 11, 11),
            responsibleId = userA.id,
            accountId = accA.id,
            type = TransactionType.TRANSFER,
            destinationAccountId = accB.id,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            generatesDebt = false
        )

        `when`(transactionRepository.findByAccountId(accB.id)).thenReturn(emptyList())
        `when`(transactionRepository.findByDestinationAccountId(accB.id)).thenReturn(listOf(pendingDebtTransfer, nonDebtTransfer))

        val response = useCase.execute(group.id, userB.id)

        assertEquals(0, response.totalOwedByMe)
        assertEquals(0, response.totalReceivableByMe)
        val monthNov = response.monthlyBalances.find { it.month == 11 && it.year == 2026 }
        assertEquals(0, monthNov?.userBalances?.size ?: 0)
    }

    @Test
    fun `should reduce debt using forgiveness settlements with isForgiveness true`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val userB = UserEntity(Name("Maria"), Name("Silva"), Email("maria@finance.com"), PasswordHash("hash"))
        val group = GroupEntity(id = UUID.randomUUID(), name = "Família")

        `when`(groupRepository.findById(group.id)).thenReturn(group)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, userA.id)).thenReturn(true)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, userB.id)).thenReturn(true)
        `when`(userRepository.findAll()).thenReturn(listOf(userA, userB))
        `when`(creditCardRepository.findAllByGroupId(group.id)).thenReturn(emptyList())

        val accA = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Conta Arthur",
            startBalance = 50000,
            balance = 50000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = group.id
        )
        val accB = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userB.id,
            name = "Conta Maria",
            startBalance = 10000,
            balance = 10000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )
        `when`(accountRepository.findAllByGroupId(group.id)).thenReturn(listOf(accA))
        `when`(accountRepository.findById(accB.id)).thenReturn(accB)

        val debtTransfer = TransactionEntity(
            title = "Ajuda aluguel",
            amount = 30000,
            date = LocalDate.of(2026, 11, 2),
            responsibleId = userA.id,
            accountId = accA.id,
            type = TransactionType.TRANSFER,
            destinationAccountId = accB.id,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            generatesDebt = true
        )
        `when`(transactionRepository.findByAccountId(accA.id)).thenReturn(listOf(debtTransfer))
        `when`(transactionRepository.findByDestinationAccountId(accA.id)).thenReturn(emptyList())

        val forgiveness = DebtSettlementEntity(
            groupId = group.id,
            debtorId = userB.id,
            creditorId = userA.id,
            amount = 12000,
            targetMonth = 11,
            targetYear = 2026,
            date = LocalDate.of(2026, 11, 10),
            transactionId = null,
            notes = "Desconto combinado",
            isForgiveness = true
        )
        `when`(debtSettlementRepository.findByGroupId(group.id)).thenReturn(listOf(forgiveness))

        val response = useCase.execute(group.id, userA.id)

        assertEquals(0, response.totalOwedByMe)
        assertEquals(18000, response.totalReceivableByMe)
        assertEquals(1, response.settlements.size)
        assertTrue(response.settlements[0].isForgiveness)
        assertFalse(response.settlements[0].transactionId != null)
        assertEquals(12000, response.settlements[0].amount)

        val monthNov = response.monthlyBalances.find { it.month == 11 && it.year == 2026 }
        assertNotNull(monthNov)
        assertEquals(18000, monthNov.userBalances[0].netAmount)
        assertEquals(12000, monthNov.userBalances[0].grossDebts[0].settledAmount)
        assertEquals(18000, monthNov.userBalances[0].grossDebts[0].remainingAmount)
    }

    @Test
    fun `should cross-compensate shared credit card purchase and generatesDebt transfer in the same month`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val userB = UserEntity(Name("Maria"), Name("Silva"), Email("maria@finance.com"), PasswordHash("hash"))
        val group = GroupEntity(id = UUID.randomUUID(), name = "Família")

        `when`(groupRepository.findById(group.id)).thenReturn(group)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, userA.id)).thenReturn(true)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, userB.id)).thenReturn(true)
        `when`(userRepository.findAll()).thenReturn(listOf(userA, userB))
        `when`(debtSettlementRepository.findByGroupId(group.id)).thenReturn(emptyList())

        // Cartão pertence a Arthur (userA), Maria faz compra de R$ 100,00 -> Maria deve 10000 para Arthur
        val cardA = CreditCardEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            accountId = UUID.randomUUID(),
            name = "Cartão Arthur",
            limitAmount = 500000,
            closingDay = 25,
            dueDay = 5,
            groupId = group.id
        )
        `when`(creditCardRepository.findAllByGroupId(group.id)).thenReturn(listOf(cardA))

        val cardTx = TransactionEntity(
            title = "Jantar no cartão",
            amount = 10000,
            date = LocalDate.of(2026, 11, 8),
            responsibleId = userB.id,
            accountId = cardA.accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            creditCardId = cardA.id,
            invoiceMonth = 11,
            invoiceYear = 2026
        )
        `when`(transactionRepository.findByCreditCardId(cardA.id)).thenReturn(listOf(cardTx))

        // Maria transfere R$ 40,00 para Arthur com generatesDebt = true -> Arthur deve 4000 para Maria
        val accA = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Conta Arthur",
            startBalance = 50000,
            balance = 50000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = group.id
        )
        val accB = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userB.id,
            name = "Conta Maria",
            startBalance = 50000,
            balance = 50000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = group.id
        )
        `when`(accountRepository.findAllByGroupId(group.id)).thenReturn(listOf(accA, accB))

        val transferTx = TransactionEntity(
            title = "Empréstimo Maria -> Arthur",
            amount = 4000,
            date = LocalDate.of(2026, 11, 15),
            responsibleId = userB.id,
            accountId = accB.id,
            type = TransactionType.TRANSFER,
            destinationAccountId = accA.id,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            generatesDebt = true
        )
        `when`(transactionRepository.findByAccountId(accA.id)).thenReturn(emptyList())
        `when`(transactionRepository.findByDestinationAccountId(accA.id)).thenReturn(listOf(transferTx))
        `when`(transactionRepository.findByAccountId(accB.id)).thenReturn(listOf(transferTx))
        `when`(transactionRepository.findByDestinationAccountId(accB.id)).thenReturn(emptyList())

        val response = useCase.execute(group.id, userA.id)

        // Compensação líquida: 10000 (Maria -> Arthur) - 4000 (Arthur -> Maria) = 6000 a receber por Arthur
        assertEquals(0, response.totalOwedByMe)
        assertEquals(6000, response.totalReceivableByMe)

        val monthNov = response.monthlyBalances.find { it.month == 11 && it.year == 2026 }
        assertNotNull(monthNov)
        assertEquals(1, monthNov.userBalances.size)
        val pairBalance = monthNov.userBalances[0]
        assertEquals(userB.id, pairBalance.debtor.id)
        assertEquals(userA.id, pairBalance.creditor.id)
        assertEquals(6000, pairBalance.netAmount)
        assertEquals(2, pairBalance.grossDebts.size)
    }

    @Test
    fun `should ignore joint accounts without throwing NullPointerException or creating debts`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val userB = UserEntity(Name("Maria"), Name("Silva"), Email("maria@finance.com"), PasswordHash("hash"))
        val group = GroupEntity(id = UUID.randomUUID(), name = "Família")

        `when`(groupRepository.findById(group.id)).thenReturn(group)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, userA.id)).thenReturn(true)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, userB.id)).thenReturn(true)
        `when`(userRepository.findAll()).thenReturn(listOf(userA, userB))
        `when`(debtSettlementRepository.findByGroupId(group.id)).thenReturn(emptyList())
        `when`(creditCardRepository.findAllByGroupId(group.id)).thenReturn(emptyList())

        val accA = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Conta Arthur",
            startBalance = 50000,
            balance = 50000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = group.id
        )
        val jointAcc = AccountEntity.createJoint(
            groupId = group.id,
            name = "Conta Conjunta",
            startBalance = 10000,
            type = AccountType.CHECKING
        )
        `when`(accountRepository.findAllByGroupId(group.id)).thenReturn(listOf(accA, jointAcc))

        val transferToJoint = TransactionEntity(
            title = "Aporte",
            amount = 10000,
            date = LocalDate.of(2026, 11, 15),
            responsibleId = userA.id,
            accountId = accA.id,
            type = TransactionType.TRANSFER,
            destinationAccountId = jointAcc.id,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            generatesDebt = true
        )
        `when`(transactionRepository.findByAccountId(accA.id)).thenReturn(listOf(transferToJoint))
        `when`(transactionRepository.findByDestinationAccountId(accA.id)).thenReturn(emptyList())
        `when`(transactionRepository.findByAccountId(jointAcc.id)).thenReturn(emptyList())
        `when`(transactionRepository.findByDestinationAccountId(jointAcc.id)).thenReturn(listOf(transferToJoint))

        val response = useCase.execute(group.id, userA.id)

        assertEquals(0, response.totalOwedByMe)
        assertEquals(0, response.totalReceivableByMe)
        assertTrue(response.monthlyBalances.all { it.userBalances.isEmpty() })
    }
}
