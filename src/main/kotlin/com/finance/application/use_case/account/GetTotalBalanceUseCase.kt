package com.finance.application.use_case.account

import com.finance.application.dto.account.TotalBalanceResponse
import com.finance.domain.repository.AccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class GetTotalBalanceUseCase(
    private val accountRepository: AccountRepository
) {

    @Transactional(readOnly = true)
    fun execute(authenticatedUserId: UUID, ignoreAccountIds: Set<UUID>?): TotalBalanceResponse {
        val accounts = accountRepository.findAllByOwnerId(authenticatedUserId)
        
        val filteredAccounts = if (ignoreAccountIds != null && ignoreAccountIds.isNotEmpty()) {
            accounts.filter { it.id !in ignoreAccountIds }
        } else {
            accounts
        }

        val total = filteredAccounts.sumOf { it.balance }

        return TotalBalanceResponse(total = total)
    }
}
