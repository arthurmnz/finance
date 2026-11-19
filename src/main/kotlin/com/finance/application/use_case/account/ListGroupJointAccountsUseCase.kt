package com.finance.application.use_case.account

import com.finance.application.dto.account.GroupAccountResponse
import com.finance.application.mapper.AccountDtoMapper
import com.finance.application.service.GroupService
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.JointAccountMemberRepository
import com.finance.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ListGroupJointAccountsUseCase(
    private val groupService: GroupService,
    private val accountRepository: AccountRepository,
    private val jointAccountMemberRepository: JointAccountMemberRepository,
    private val userRepository: UserRepository,
    private val accountDtoMapper: AccountDtoMapper
) {

    @Transactional(readOnly = true)
    fun execute(groupId: UUID, authenticatedUserId: UUID): List<GroupAccountResponse> {
        groupService.findGroupById(groupId)
        val membership = groupService.findMember(groupId, authenticatedUserId)
        groupService.requireOwnerOrAdmin(membership)

        val jointAccounts = accountRepository.findAllByGroupIdAndIsJointTrue(groupId)
        if (jointAccounts.isEmpty()) return emptyList()

        val membersByAccountId = jointAccountMemberRepository
            .findAllByAccountIdIn(jointAccounts.map { it.id })
            .groupBy { it.accountId }

        val userIds = membersByAccountId.values.flatten().map { it.userId }.distinct()
        val usersById = userRepository.findAllByIdIn(userIds).associateBy { it.id }

        return jointAccounts.map { account ->
            val allowedMembers = accountDtoMapper.toSortedUserSummaries(
                membersByAccountId[account.id].orEmpty().mapNotNull { usersById[it.userId] }
            )

            accountDtoMapper.toGroupAccountResponse(
                account = account,
                owner = null,
                allowedMembers = allowedMembers
            )
        }
    }
}
