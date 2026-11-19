package com.finance.application.dto.account

import com.finance.application.dto.group.GroupUserSummary
import com.finance.domain.enum.AccountType
import java.util.UUID

data class CreateJointAccountRequest(
    val name: String,
    val startBalance: Int,
    val type: AccountType,
    val tax: Int? = null,
    val includeInTotal: Boolean = true,
    val memberIds: List<UUID>
)

data class UpdateJointAccountRequest(
    val name: String,
    val startBalance: Int,
    val balance: Int? = null,
    val type: AccountType,
    val tax: Int? = null,
    val includeInTotal: Boolean = true,
    val memberIds: List<UUID>
)

data class JointAccountMemberShareResponse(
    val user: GroupUserSummary,
    val shareAmount: Long,
    val contributedAmount: Long,
    val launchedExpensesAmount: Long,
    val netBalance: Long
)

data class JointAccountSummaryResponse(
    val accountId: UUID,
    val accountName: String,
    val groupId: UUID,
    val currentBalance: Long,
    val month: Int,
    val year: Int,
    val totalExpenses: Long,
    val completedExpenses: Long,
    val pendingExpenses: Long,
    val totalIncome: Long,
    val totalContributions: Long,
    val memberCount: Int,
    val memberShares: List<JointAccountMemberShareResponse>
)
