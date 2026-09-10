package com.finance.application.use_case.account

import com.finance.application.dto.account.TotalBalanceResponse
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.enum.TransactionType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class GetTotalBalanceUseCase(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) {

    @Transactional(readOnly = true)
    fun execute(
        authenticatedUserId: UUID, 
        ignoreAccountIds: Set<UUID>?,
        month: Int? = null,
        year: Int? = null
    ): TotalBalanceResponse {
        month?.let {
            if (month !in 1..12)
                throw IllegalArgumentException("O mês deve estar entre 1 e 12.")
        }

        val accounts = accountRepository.findAllByOwnerId(authenticatedUserId)
        
        val filteredAccounts = if (!ignoreAccountIds.isNullOrEmpty()) {
            accounts.filter { it.id !in ignoreAccountIds }
        } else {
            accounts
        }

        val total = filteredAccounts.sumOf { it.balance }

        val accountIds = filteredAccounts.map { it.id }
        
        var monthlyIncome = 0
        var monthlyExpense = 0

        if (accountIds.isNotEmpty()) {
            val now = LocalDate.now()
            val targetYear = year ?: now.year
            val targetMonth = month ?: now.monthValue
            
            val startDate = LocalDate.of(targetYear, targetMonth, 1)
            val endDate = startDate.withDayOfMonth(startDate.lengthOfMonth())

            val transactions = transactionRepository.findByAccountIdInAndDateBetween(accountIds, startDate, endDate)
            
            monthlyIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            monthlyExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        }

        return TotalBalanceResponse(
            total = total,
            monthlyIncome = monthlyIncome,
            monthlyExpense = monthlyExpense
        )
    }
}
