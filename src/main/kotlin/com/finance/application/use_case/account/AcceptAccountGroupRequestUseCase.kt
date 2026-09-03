package com.finance.application.use_case.account

import com.finance.application.service.GroupService
import com.finance.domain.exception.AccountAlreadyLinkedException
import com.finance.domain.exception.AccountGroupRequestNotFoundException
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.repository.AccountGroupRequestRepository
import com.finance.domain.repository.AccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AcceptAccountGroupRequestUseCase(
    private val accountGroupRequestRepository: AccountGroupRequestRepository,
    private val accountRepository: AccountRepository,
    private val groupService: GroupService
) {

    @Transactional
    fun execute(requestId: UUID, groupId: UUID, authenticatedUserId: UUID) {
        val request = accountGroupRequestRepository.findById(requestId) ?: throw AccountGroupRequestNotFoundException()
        require(request.groupId == groupId) { "Solicitação não pertence a este grupo." }

        val membership = groupService.findMember(groupId, authenticatedUserId)
        groupService.requireOwnerOrAdmin(membership)

        request.accept()
        accountGroupRequestRepository.save(request)

        val account = accountRepository.findById(request.accountId) ?: throw AccountNotFoundException()
        if (account.groupId != null) {
            throw AccountAlreadyLinkedException() // Caso a conta tenha sido vinculada por outro meio
        }

        account.linkToGroup(groupId)
        accountRepository.save(account)
    }
}
