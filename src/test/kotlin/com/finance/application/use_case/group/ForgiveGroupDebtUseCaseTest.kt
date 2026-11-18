package com.finance.application.use_case.group

import com.finance.application.dto.group.ForgiveDebtRequest
import com.finance.application.dto.group.GroupDebtsResponse
import com.finance.application.dto.group.GroupMonthlyDebtResponse
import com.finance.application.dto.group.GroupUserSummary
import com.finance.application.dto.group.PairDebtBalanceResponse
import com.finance.domain.entity.DebtSettlementEntity
import com.finance.domain.entity.GroupEntity
import com.finance.domain.entity.UserEntity
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.GroupNotFoundException
import com.finance.domain.exception.InvalidTransactionException
import com.finance.domain.repository.DebtSettlementRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
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
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ForgiveGroupDebtUseCaseTest {

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(): T {
        Mockito.any<T>()
        return null as T
    }

    private lateinit var groupRepository: GroupRepository
    private lateinit var groupMemberRepository: GroupMemberRepository
    private lateinit var debtSettlementRepository: DebtSettlementRepository
    private lateinit var userRepository: UserRepository
    private lateinit var calculateGroupDebtsUseCase: CalculateGroupDebtsUseCase
    private lateinit var useCase: ForgiveGroupDebtUseCase

    @BeforeEach
    fun setup() {
        groupRepository = mock(GroupRepository::class.java)
        groupMemberRepository = mock(GroupMemberRepository::class.java)
        debtSettlementRepository = mock(DebtSettlementRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        calculateGroupDebtsUseCase = mock(CalculateGroupDebtsUseCase::class.java)

        useCase = ForgiveGroupDebtUseCase(
            groupRepository = groupRepository,
            groupMemberRepository = groupMemberRepository,
            debtSettlementRepository = debtSettlementRepository,
            userRepository = userRepository,
            calculateGroupDebtsUseCase = calculateGroupDebtsUseCase
        )
    }

    private fun summary(u: UserEntity) = GroupUserSummary(
        id = u.id,
        firstName = u.firstName.value,
        lastName = u.lastName.value,
        email = u.email.normalized
    )

    private fun setupDebts(
        group: GroupEntity,
        creditor: UserEntity,
        debtor: UserEntity,
        month: Int,
        year: Int,
        netAmount: Int
    ) {
        `when`(groupRepository.findById(group.id)).thenReturn(group)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, creditor.id)).thenReturn(true)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, debtor.id)).thenReturn(true)
        `when`(userRepository.findById(creditor.id)).thenReturn(creditor)
        `when`(userRepository.findById(debtor.id)).thenReturn(debtor)

        val debtsResponse = GroupDebtsResponse(
            groupId = group.id,
            totalOwedByMe = 0,
            totalReceivableByMe = netAmount,
            monthlyBalances = listOf(
                GroupMonthlyDebtResponse(
                    month = month,
                    year = year,
                    userBalances = listOf(
                        PairDebtBalanceResponse(
                            debtor = summary(debtor),
                            creditor = summary(creditor),
                            netAmount = netAmount,
                            grossDebts = emptyList()
                        )
                    )
                )
            ),
            settlements = emptyList()
        )
        `when`(calculateGroupDebtsUseCase.execute(group.id, creditor.id)).thenReturn(debtsResponse)
        `when`(calculateGroupDebtsUseCase.execute(group.id, debtor.id)).thenReturn(debtsResponse)
    }

    @Test
    fun `should forgive partial debt without transactionId and with isForgiveness true`() {
        val creditor = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val debtor = UserEntity(Name("Maria"), Name("Silva"), Email("maria@finance.com"), PasswordHash("hash"))
        val group = GroupEntity(id = UUID.randomUUID(), name = "Família")

        setupDebts(group, creditor, debtor, month = 11, year = 2026, netAmount = 15000)
        var capturedSettlement: DebtSettlementEntity? = null
        `when`(debtSettlementRepository.save(anyNonNull())).thenAnswer {
            val entity = it.getArgument<DebtSettlementEntity>(0)
            capturedSettlement = entity
            entity
        }

        val request = ForgiveDebtRequest(
            debtorId = debtor.id,
            amount = 5000,
            targetMonth = 11,
            targetYear = 2026,
            notes = "  Desconto parcial  "
        )

        val response = useCase.execute(group.id, creditor.id, request)

        assertNotNull(response)
        assertTrue(response.isForgiveness)
        assertNull(response.transactionId)
        assertEquals(5000, response.amount)
        assertEquals(debtor.id, response.debtorId)
        assertEquals(creditor.id, response.creditorId)
        assertEquals(11, response.targetMonth)
        assertEquals(2026, response.targetYear)
        assertEquals("Desconto parcial", response.notes)

        verify(debtSettlementRepository).save(anyNonNull())
        val saved = assertNotNull(capturedSettlement)
        assertTrue(saved.isForgiveness)
        assertNull(saved.transactionId)
    }

    @Test
    fun `should forgive full debt using default notes when notes is blank`() {
        val creditor = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val debtor = UserEntity(Name("Maria"), Name("Silva"), Email("maria@finance.com"), PasswordHash("hash"))
        val group = GroupEntity(id = UUID.randomUUID(), name = "Família")

        setupDebts(group, creditor, debtor, month = 11, year = 2026, netAmount = 15000)
        `when`(debtSettlementRepository.save(anyNonNull())).thenAnswer { it.arguments[0] }

        val request = ForgiveDebtRequest(
            debtorId = debtor.id,
            amount = 15000,
            targetMonth = 11,
            targetYear = 2026,
            notes = "   "
        )

        val response = useCase.execute(group.id, creditor.id, request)

        assertTrue(response.isForgiveness)
        assertNull(response.transactionId)
        assertEquals(15000, response.amount)
        assertEquals("Perdão de dívida", response.notes)
    }

    @Test
    fun `should reject when debtor tries to forgive own debt owed to creditor`() {
        val creditor = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val debtor = UserEntity(Name("Maria"), Name("Silva"), Email("maria@finance.com"), PasswordHash("hash"))
        val group = GroupEntity(id = UUID.randomUUID(), name = "Família")

        setupDebts(group, creditor, debtor, month = 11, year = 2026, netAmount = 15000)

        // Maria (devedora) tenta chamar perdão passando Arthur como debtorId
        val request = ForgiveDebtRequest(
            debtorId = creditor.id,
            amount = 5000,
            targetMonth = 11,
            targetYear = 2026
        )

        assertThrows<InvalidTransactionException> {
            useCase.execute(group.id, debtor.id, request)
        }
        verifyNoInteractions(debtSettlementRepository)
    }

    @Test
    fun `should reject when user tries to forgive debt with themselves`() {
        val user = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val groupId = UUID.randomUUID()

        val request = ForgiveDebtRequest(
            debtorId = user.id,
            amount = 5000,
            targetMonth = 11,
            targetYear = 2026
        )

        assertThrows<InvalidTransactionException> {
            useCase.execute(groupId, user.id, request)
        }
        verifyNoInteractions(debtSettlementRepository)
    }

    @Test
    fun `should reject when forgiveness amount exceeds netAmount`() {
        val creditor = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val debtor = UserEntity(Name("Maria"), Name("Silva"), Email("maria@finance.com"), PasswordHash("hash"))
        val group = GroupEntity(id = UUID.randomUUID(), name = "Família")

        setupDebts(group, creditor, debtor, month = 11, year = 2026, netAmount = 15000)

        val request = ForgiveDebtRequest(
            debtorId = debtor.id,
            amount = 15001,
            targetMonth = 11,
            targetYear = 2026
        )

        assertThrows<InvalidTransactionException> {
            useCase.execute(group.id, creditor.id, request)
        }
        verifyNoInteractions(debtSettlementRepository)
    }

    @Test
    fun `should reject invalid amount, month, year or overly long notes`() {
        val creditorId = UUID.randomUUID()
        val debtorId = UUID.randomUUID()
        val groupId = UUID.randomUUID()

        assertThrows<InvalidTransactionException> {
            useCase.execute(groupId, creditorId, ForgiveDebtRequest(debtorId, 0, 11, 2026))
        }
        assertThrows<InvalidTransactionException> {
            useCase.execute(groupId, creditorId, ForgiveDebtRequest(debtorId, 1000, 13, 2026))
        }
        assertThrows<InvalidTransactionException> {
            useCase.execute(groupId, creditorId, ForgiveDebtRequest(debtorId, 1000, 11, 1969))
        }
        assertThrows<InvalidTransactionException> {
            useCase.execute(groupId, creditorId, ForgiveDebtRequest(debtorId, 1000, 11, 2026, "a".repeat(501)))
        }
    }

    @Test
    fun `should enforce group existence and membership access checks`() {
        val creditorId = UUID.randomUUID()
        val debtorId = UUID.randomUUID()
        val group = GroupEntity(id = UUID.randomUUID(), name = "Família")
        val request = ForgiveDebtRequest(debtorId, 5000, 11, 2026)

        `when`(groupRepository.findById(group.id)).thenReturn(null)
        assertThrows<GroupNotFoundException> {
            useCase.execute(group.id, creditorId, request)
        }

        `when`(groupRepository.findById(group.id)).thenReturn(group)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, creditorId)).thenReturn(false)
        assertThrows<AccessDeniedException> {
            useCase.execute(group.id, creditorId, request)
        }

        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, creditorId)).thenReturn(true)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, debtorId)).thenReturn(false)
        assertThrows<AccessDeniedException> {
            useCase.execute(group.id, creditorId, request)
        }
    }
}
