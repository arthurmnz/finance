package com.finance.application.dto.admin

import com.finance.domain.enum.UserRole
import java.time.LocalDateTime
import java.util.UUID

data class AdminMonthlyGrowth(
    val month: String,
    val label: String,
    val usersCount: Long,
    val transactionsCount: Long,
)

data class AdminMetricsResponse(
    val totalUsers: Long,
    val activeUsers: Long,
    val blockedUsers: Long,
    val adminUsers: Long,
    val newUsersLast7Days: Long,
    val newUsersLast30Days: Long,
    val totalAccounts: Long,
    val totalCreditCards: Long,
    val totalGroups: Long,
    val totalTransactions: Long,
    val monthlyGrowth: List<AdminMonthlyGrowth>,
)

data class AdminUserResponse(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: UserRole,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val accountsCount: Long,
    val creditCardsCount: Long,
    val groupsCount: Long,
    val transactionsCount: Long,
)
