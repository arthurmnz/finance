package com.finance.application.use_case.account

import com.finance.application.dto.account.AccountResponse
import com.finance.application.dto.account.UpdateAccountRequest
import com.finance.application.mapper.AccountDtoMapper
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.repository.AccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UpdateAccountUseCase(
    private val accountRepository: AccountRepository,
    private val accountDtoMapper: AccountDtoMapper
) {

    @Transactional
    fun execute(accountId: UUID, request: UpdateAccountRequest, authenticatedUserId: UUID): AccountResponse {
        val account = accountRepository.findById(accountId) ?: throw AccountNotFoundException()

        if (account.ownerId != authenticatedUserId) {
            throw AccessDeniedException("Você só pode editar suas próprias contas.")
        }

        account.update(
            newName = request.name,
            newStartBalance = request.startBalance,
            newType = request.type,
            newTax = request.tax
        )

        val saved = accountRepository.save(account)
        return accountDtoMapper.toAccountResponse(saved)
    }
}
