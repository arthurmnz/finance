package com.finance.application.use_case.account

import com.finance.application.dto.account.AccountGroupRequestResponse
import com.finance.application.mapper.AccountDtoMapper
import com.finance.application.service.GroupService
import com.finance.application.service.UserService
import com.finance.domain.enum.AccountGroupRequestStatus
import com.finance.domain.repository.AccountGroupRequestRepository
import com.finance.domain.repository.AccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ListPendingAccountGroupRequestsUseCase(
    private val groupService: GroupService,
    private val accountGroupRequestRepository: AccountGroupRequestRepository,
    private val accountRepository: AccountRepository,
    private val userService: UserService,
    private val accountDtoMapper: AccountDtoMapper
) {

    @Transactional(readOnly = true)
    fun execute(groupId: UUID, authenticatedUserId: UUID): List<AccountGroupRequestResponse> {
        val membership = groupService.findMember(groupId, authenticatedUserId)
        groupService.requireOwnerOrAdmin(membership)

        return accountGroupRequestRepository
            .findAllByGroupIdAndStatus(groupId, AccountGroupRequestStatus.PENDING)
            .map { request ->
                val account = accountRepository.findById(request.accountId)!!
                val requester = userService.findById(request.requesterId)
                accountDtoMapper.toAccountGroupRequestResponse(request, account, requester)
            }
    }
}
