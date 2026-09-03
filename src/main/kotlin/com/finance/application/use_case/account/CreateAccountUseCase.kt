package com.finance.application.use_case.account

import com.finance.application.dto.account.AccountResponse
import com.finance.application.dto.account.CreateAccountRequest
import com.finance.application.mapper.AccountDtoMapper
import com.finance.domain.entity.AccountEntity
import com.finance.domain.repository.AccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CreateAccountUseCase(
    private val accountRepository: AccountRepository,
    private val accountDtoMapper: AccountDtoMapper
) {

    @Transactional
    fun execute(request: CreateAccountRequest, authenticatedUserId: UUID): AccountResponse {
        val account = AccountEntity(
            ownerId = authenticatedUserId,
            name = request.name,
            startBalance = request.startBalance,
            type = request.type,
            tax = request.tax
        )

        val saved = accountRepository.save(account)
        return accountDtoMapper.toAccountResponse(saved)
    }
}
