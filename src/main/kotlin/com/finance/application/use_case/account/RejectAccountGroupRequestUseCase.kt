package com.finance.application.use_case.account

import com.finance.application.service.GroupService
import com.finance.domain.exception.AccountGroupRequestNotFoundException
import com.finance.domain.repository.AccountGroupRequestRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RejectAccountGroupRequestUseCase(
    private val accountGroupRequestRepository: AccountGroupRequestRepository,
    private val groupService: GroupService
) {

    @Transactional
    fun execute(requestId: UUID, groupId: UUID, authenticatedUserId: UUID) {
        val request = accountGroupRequestRepository.findById(requestId) ?: throw AccountGroupRequestNotFoundException()
        require(request.groupId == groupId) { "Solicitação não pertence a este grupo." }

        val membership = groupService.findMember(groupId, authenticatedUserId)
        groupService.requireOwnerOrAdmin(membership)

        request.reject()
        accountGroupRequestRepository.save(request)
    }
}
