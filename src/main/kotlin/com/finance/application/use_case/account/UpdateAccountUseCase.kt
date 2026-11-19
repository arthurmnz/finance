package com.finance.application.use_case.account

import com.finance.application.dto.account.AccountResponse
import com.finance.application.dto.account.UpdateAccountRequest
import com.finance.application.mapper.AccountDtoMapper
import com.finance.application.service.AccountAccessService
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.JointAccountMemberRepository
import com.finance.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UpdateAccountUseCase(
    private val accountRepository: AccountRepository,
    private val accountDtoMapper: AccountDtoMapper,
    private val accountAccessService: AccountAccessService,
    private val jointAccountMemberRepository: JointAccountMemberRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun execute(accountId: UUID, request: UpdateAccountRequest, authenticatedUserId: UUID): AccountResponse {
        val account = accountRepository.findById(accountId) ?: throw AccountNotFoundException()

        accountAccessService.requireManage(account, authenticatedUserId, "Você não tem permissão para editar esta conta.")

        if (account.isJoint) {
            account.updateJoint(
                newName = request.name,
                newStartBalance = request.startBalance,
                newBalance = null,
                newType = request.type,
                newTax = request.tax,
                newIncludeInTotal = request.includeInTotal
            )
        } else {
            account.update(
                newName = request.name,
                newStartBalance = request.startBalance,
                newType = request.type,
                newTax = request.tax,
                newIncludeInTotal = request.includeInTotal
            )
        }

        val saved = accountRepository.save(account)
        val allowedMembers = if (saved.isJoint) {
            val userIds = jointAccountMemberRepository.findAllByAccountId(saved.id).map { it.userId }.distinct()
            accountDtoMapper.toSortedUserSummaries(userRepository.findAllByIdIn(userIds))
        } else {
            emptyList()
        }
        return accountDtoMapper.toAccountResponse(saved, allowedMembers)
    }
}
