package com.finance.application.use_case.account

import com.finance.application.dto.account.AccountResponse
import com.finance.application.mapper.AccountDtoMapper
import com.finance.domain.repository.AccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ListMyAccountsUseCase(
    private val accountRepository: AccountRepository,
    private val accountDtoMapper: AccountDtoMapper
) {

    @Transactional(readOnly = true)
    fun execute(authenticatedUserId: UUID): List<AccountResponse> {
        return accountRepository.findAllByOwnerId(authenticatedUserId)
            .map { accountDtoMapper.toAccountResponse(it) }
    }
}
