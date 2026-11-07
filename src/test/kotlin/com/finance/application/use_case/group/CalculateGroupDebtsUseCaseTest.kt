package com.finance.application.use_case.group

import com.finance.domain.entity.CreditCardEntity
import com.finance.domain.entity.DebtSettlementEntity
import com.finance.domain.entity.GroupEntity
import com.finance.domain.entity.GroupMemberEntity
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.entity.UserEntity
import com.finance.domain.enum.GroupRole
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
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
import kotlin.test.assertNotNull

class CalculateGroupDebtsUseCaseTest {

    private lateinit var groupRepository: GroupRepository
    private lateinit var groupMemberRepository: GroupMemberRepository
    private lateinit var creditCardRepository: CreditCardRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var debtSettlementRepository: DebtSettlementRepository
    private lateinit var userRepository: UserRepository
    private lateinit var useCase: CalculateGroupDebtsUseCase

    @BeforeEach
    fun setup() {
        groupRepository = mock(GroupRepository::class.java)
        groupMemberRepository = mock(GroupMemberRepository::class.java)
        creditCardRepository = mock(CreditCardRepository::class.java)
        transactionRepository = mock(TransactionRepository::class.java)
        debtSettlementRepository = mock(DebtSettlementRepository::class.java)
        userRepository = mock(UserRepository::class.java)

        useCase = CalculateGroupDebtsUseCase(
            groupRepository = groupRepository,
            groupMemberRepository = groupMemberRepository,
            creditCardRepository = creditCardRepository,
            transactionRepository = transactionRepository,
            debtSettlementRepository = debtSettlementRepository,
            userRepository = userRepository
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
}
