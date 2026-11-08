package com.finance.application.use_case.user

import com.finance.application.dto.group.UserDebtGroupSummary
import com.finance.application.dto.group.UserDebtsSummaryResponse
import com.finance.application.use_case.group.CalculateGroupDebtsUseCase
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth
import java.util.UUID

@Service
class GetUserDebtsSummaryUseCase(
    private val groupMemberRepository: GroupMemberRepository,
    private val groupRepository: GroupRepository,
    private val calculateGroupDebtsUseCase: CalculateGroupDebtsUseCase
) {
    @Transactional(readOnly = true)
    fun execute(userId: UUID, month: Int? = null, year: Int? = null): UserDebtsSummaryResponse {
        val targetYm = if (month != null && year != null) {
            if (month !in 1..12) throw IllegalArgumentException("O mês deve estar entre 1 e 12.")
            YearMonth.of(year, month)
        } else {
            YearMonth.now()
        }

        val memberships = groupMemberRepository.findAllByUserId(userId)

        val groupSummaries = mutableListOf<UserDebtGroupSummary>()
        var totalOwedCurrent = 0
        var totalReceivableCurrent = 0
        var totalOwedAll = 0
        var totalReceivableAll = 0

        for (member in memberships) {
            val group = groupRepository.findById(member.groupId) ?: continue
            val debts = calculateGroupDebtsUseCase.execute(group.id, userId)

            val currentMonthDebt = debts.monthlyBalances.find {
                it.month == targetYm.monthValue && it.year == targetYm.year
            }

            val owedCurrent = currentMonthDebt?.userBalances
                ?.filter { it.debtor.id == userId }
                ?.sumOf { it.netAmount } ?: 0

            val receivableCurrent = currentMonthDebt?.userBalances
                ?.filter { it.creditor.id == userId }
                ?.sumOf { it.netAmount } ?: 0

            val owedTotal = debts.totalOwedByMe
            val receivableTotal = debts.totalReceivableByMe

            totalOwedCurrent += owedCurrent
            totalReceivableCurrent += receivableCurrent
            totalOwedAll += owedTotal
            totalReceivableAll += receivableTotal

            if (owedTotal > 0 || receivableTotal > 0 || owedCurrent > 0 || receivableCurrent > 0) {
                groupSummaries.add(
                    UserDebtGroupSummary(
                        groupId = group.id,
                        groupName = group.name,
                        owedCurrentMonth = owedCurrent,
                        receivableCurrentMonth = receivableCurrent,
                        owedTotal = owedTotal,
                        receivableTotal = receivableTotal
                    )
                )
            }
        }

        return UserDebtsSummaryResponse(
            totalOwedCurrentMonth = totalOwedCurrent,
            totalReceivableCurrentMonth = totalReceivableCurrent,
            totalOwedAllMonths = totalOwedAll,
            totalReceivableAllMonths = totalReceivableAll,
            groups = groupSummaries
        )
    }
}
