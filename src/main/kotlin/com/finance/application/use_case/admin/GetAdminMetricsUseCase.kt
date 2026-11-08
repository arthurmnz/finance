package com.finance.application.use_case.admin

import com.finance.application.dto.admin.AdminMetricsResponse
import com.finance.application.dto.admin.AdminMonthlyGrowth
import com.finance.domain.enum.UserRole
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Service
class GetAdminMetricsUseCase(
    private val entityManager: EntityManager
) {
    @Transactional(readOnly = true)
    fun execute(): AdminMetricsResponse {
        val now = LocalDateTime.now()
        val sevenDaysAgo = now.minusDays(7)
        val thirtyDaysAgo = now.minusDays(30)

        val totalUsers = entityManager.createQuery(
            "SELECT COUNT(u) FROM UserJpaEntity u", Long::class.javaObjectType
        ).singleResult ?: 0L

        val activeUsers = entityManager.createQuery(
            "SELECT COUNT(u) FROM UserJpaEntity u WHERE u.isActive = true", Long::class.javaObjectType
        ).singleResult ?: 0L

        val blockedUsers = entityManager.createQuery(
            "SELECT COUNT(u) FROM UserJpaEntity u WHERE u.isActive = false", Long::class.javaObjectType
        ).singleResult ?: 0L

        val adminUsers = entityManager.createQuery(
            "SELECT COUNT(u) FROM UserJpaEntity u WHERE u.role = :role", Long::class.javaObjectType
        ).setParameter("role", UserRole.ADMIN).singleResult ?: 0L

        val newUsersLast7Days = entityManager.createQuery(
            "SELECT COUNT(u) FROM UserJpaEntity u WHERE u.createdAt >= :date", Long::class.javaObjectType
        ).setParameter("date", sevenDaysAgo).singleResult ?: 0L

        val newUsersLast30Days = entityManager.createQuery(
            "SELECT COUNT(u) FROM UserJpaEntity u WHERE u.createdAt >= :date", Long::class.javaObjectType
        ).setParameter("date", thirtyDaysAgo).singleResult ?: 0L

        val totalAccounts = entityManager.createQuery(
            "SELECT COUNT(a) FROM AccountJpaEntity a", Long::class.javaObjectType
        ).singleResult ?: 0L

        val totalCreditCards = entityManager.createQuery(
            "SELECT COUNT(c) FROM CreditCardJpaEntity c", Long::class.javaObjectType
        ).singleResult ?: 0L

        val totalGroups = entityManager.createQuery(
            "SELECT COUNT(g) FROM GroupJpaEntity g", Long::class.javaObjectType
        ).singleResult ?: 0L

        val totalTransactions = entityManager.createQuery(
            "SELECT COUNT(t) FROM TransactionJpaEntity t", Long::class.javaObjectType
        ).singleResult ?: 0L

        val monthlyGrowth = mutableListOf<AdminMonthlyGrowth>()
        val ptBr = Locale("pt", "BR")
        val currentYearMonth = LocalDate.now()

        for (i in 5 downTo 0) {
            val date = currentYearMonth.minusMonths(i.toLong())
            val startOfMonth = date.withDayOfMonth(1).atStartOfDay()
            val endOfMonth = startOfMonth.plusMonths(1)

            val usersCount = entityManager.createQuery(
                "SELECT COUNT(u) FROM UserJpaEntity u WHERE u.createdAt >= :start AND u.createdAt < :end",
                Long::class.javaObjectType
            ).setParameter("start", startOfMonth).setParameter("end", endOfMonth).singleResult ?: 0L

            val txsCount = entityManager.createQuery(
                "SELECT COUNT(t) FROM TransactionJpaEntity t WHERE t.createdAt >= :start AND t.createdAt < :end",
                Long::class.javaObjectType
            ).setParameter("start", startOfMonth).setParameter("end", endOfMonth).singleResult ?: 0L

            val monthCode = String.format("%04d-%02d", date.year, date.monthValue)
            val monthLabel = date.format(DateTimeFormatter.ofPattern("MMM/yy", ptBr))
                .replaceFirstChar { it.uppercase() }

            monthlyGrowth.add(
                AdminMonthlyGrowth(
                    month = monthCode,
                    label = monthLabel,
                    usersCount = usersCount,
                    transactionsCount = txsCount,
                )
            )
        }

        return AdminMetricsResponse(
            totalUsers = totalUsers,
            activeUsers = activeUsers,
            blockedUsers = blockedUsers,
            adminUsers = adminUsers,
            newUsersLast7Days = newUsersLast7Days,
            newUsersLast30Days = newUsersLast30Days,
            totalAccounts = totalAccounts,
            totalCreditCards = totalCreditCards,
            totalGroups = totalGroups,
            totalTransactions = totalTransactions,
            monthlyGrowth = monthlyGrowth,
        )
    }
}
