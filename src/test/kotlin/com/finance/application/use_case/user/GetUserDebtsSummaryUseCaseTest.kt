package com.finance.application.use_case.user

import com.finance.application.dto.group.GroupDebtsResponse
import com.finance.application.dto.group.GroupMonthlyDebtResponse
import com.finance.application.dto.group.GroupUserSummary
import com.finance.application.dto.group.PairDebtBalanceResponse
import com.finance.application.use_case.group.CalculateGroupDebtsUseCase
import com.finance.domain.entity.GroupEntity
import com.finance.domain.entity.GroupMemberEntity
import com.finance.domain.enum.GroupRole
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.util.UUID
import kotlin.test.assertEquals

class GetUserDebtsSummaryUseCaseTest {

    private lateinit var groupMemberRepository: GroupMemberRepository
    private lateinit var groupRepository: GroupRepository
    private lateinit var calculateGroupDebtsUseCase: CalculateGroupDebtsUseCase
    private lateinit var useCase: GetUserDebtsSummaryUseCase

    @BeforeEach
    fun setup() {
        groupMemberRepository = mock(GroupMemberRepository::class.java)
        groupRepository = mock(GroupRepository::class.java)
        calculateGroupDebtsUseCase = mock(CalculateGroupDebtsUseCase::class.java)

        useCase = GetUserDebtsSummaryUseCase(
            groupMemberRepository = groupMemberRepository,
            groupRepository = groupRepository,
            calculateGroupDebtsUseCase = calculateGroupDebtsUseCase
        )
    }

    @Test
    fun `should return debts summary for specified month and year`() {
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val group = GroupEntity(id = UUID.randomUUID(), name = "Família")

        val membership = GroupMemberEntity(
            id = UUID.randomUUID(),
            groupId = group.id,
            userId = userId,
            role = GroupRole.MEMBER
        )
        `when`(groupMemberRepository.findAllByUserId(userId)).thenReturn(listOf(membership))
        `when`(groupRepository.findById(group.id)).thenReturn(group)

        val balanceOct = PairDebtBalanceResponse(
            debtor = GroupUserSummary(userId, "Arthur", "Menezes", "arthur@test.com"),
            creditor = GroupUserSummary(otherUserId, "Maria", "Silva", "maria@test.com"),
            netAmount = 4500,
            grossDebts = emptyList()
        )
        val monthlyOct = GroupMonthlyDebtResponse(
            month = 10,
            year = 2026,
            userBalances = listOf(balanceOct)
        )

        val balanceNov = PairDebtBalanceResponse(
            debtor = GroupUserSummary(otherUserId, "Maria", "Silva", "maria@test.com"),
            creditor = GroupUserSummary(userId, "Arthur", "Menezes", "arthur@test.com"),
            netAmount = 1000,
            grossDebts = emptyList()
        )
        val monthlyNov = GroupMonthlyDebtResponse(
            month = 11,
            year = 2026,
            userBalances = listOf(balanceNov)
        )

        val debtsResponse = GroupDebtsResponse(
            groupId = group.id,
            totalOwedByMe = 4500,
            totalReceivableByMe = 1000,
            monthlyBalances = listOf(monthlyOct, monthlyNov),
            settlements = emptyList()
        )
        `when`(calculateGroupDebtsUseCase.execute(group.id, userId)).thenReturn(debtsResponse)

        // Consulta especificando outubro de 2026
        val summaryOct = useCase.execute(userId, month = 10, year = 2026)

        assertEquals(4500, summaryOct.totalOwedCurrentMonth)
        assertEquals(0, summaryOct.totalReceivableCurrentMonth)
        assertEquals(4500, summaryOct.totalOwedAllMonths)
        assertEquals(1000, summaryOct.totalReceivableAllMonths)
        assertEquals(1, summaryOct.groups.size)
        assertEquals(4500, summaryOct.groups[0].owedCurrentMonth)
        assertEquals(0, summaryOct.groups[0].receivableCurrentMonth)

        // Consulta especificando novembro de 2026
        val summaryNov = useCase.execute(userId, month = 11, year = 2026)
        assertEquals(0, summaryNov.totalOwedCurrentMonth)
        assertEquals(1000, summaryNov.totalReceivableCurrentMonth)
        assertEquals(4500, summaryNov.totalOwedAllMonths)
        assertEquals(1000, summaryNov.totalReceivableAllMonths)
    }

    @Test
    fun `should throw IllegalArgumentException when month is outside valid range`() {
        val userId = UUID.randomUUID()
        assertThrows<IllegalArgumentException> {
            useCase.execute(userId, month = 13, year = 2026)
        }
        assertThrows<IllegalArgumentException> {
            useCase.execute(userId, month = 0, year = 2026)
        }
    }
}
