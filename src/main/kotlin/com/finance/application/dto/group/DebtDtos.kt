package com.finance.application.dto.group

import com.finance.application.dto.transaction.TransactionResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class DebtSettlementResponse(
    val id: UUID,
    val groupId: UUID,
    val debtorId: UUID,
    val debtor: GroupUserSummary?,
    val creditorId: UUID,
    val creditor: GroupUserSummary?,
    val amount: Int,
    val targetMonth: Int,
    val targetYear: Int,
    val date: LocalDate,
    val transactionId: UUID?,
    val notes: String?,
    val createdAt: LocalDateTime
)

data class SettleDebtRequest(
    val creditorId: UUID,
    val amount: Int,
    val targetMonth: Int,
    val targetYear: Int,
    val sourceAccountId: UUID,
    val destinationAccountId: UUID,
    val notes: String? = null
)

data class MemberDebtDetailResponse(
    val debtorId: UUID,
    val creditorId: UUID,
    val grossAmount: Int,
    val settledAmount: Int,
    val remainingAmount: Int,
    val transactions: List<TransactionResponse>
)

data class PairDebtBalanceResponse(
    val debtor: GroupUserSummary,
    val creditor: GroupUserSummary,
    val netAmount: Int,
    val grossDebts: List<MemberDebtDetailResponse>
)

data class GroupMonthlyDebtResponse(
    val month: Int,
    val year: Int,
    val userBalances: List<PairDebtBalanceResponse>
)

data class GroupDebtsResponse(
    val groupId: UUID,
    val totalOwedByMe: Int,
    val totalReceivableByMe: Int,
    val monthlyBalances: List<GroupMonthlyDebtResponse>,
    val settlements: List<DebtSettlementResponse>
)

data class UserDebtGroupSummary(
    val groupId: UUID,
    val groupName: String,
    val owedCurrentMonth: Int,
    val receivableCurrentMonth: Int,
    val owedTotal: Int,
    val receivableTotal: Int
)

data class UserDebtsSummaryResponse(
    val totalOwedCurrentMonth: Int,
    val totalReceivableCurrentMonth: Int,
    val totalOwedAllMonths: Int,
    val totalReceivableAllMonths: Int,
    val groups: List<UserDebtGroupSummary>
)
