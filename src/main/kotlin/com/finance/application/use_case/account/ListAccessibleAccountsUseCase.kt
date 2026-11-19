package com.finance.application.use_case.account

import com.finance.application.dto.account.AccountResponse
import com.finance.application.mapper.AccountDtoMapper
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.JointAccountMemberRepository
import com.finance.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ListAccessibleAccountsUseCase(
    private val accountRepository: AccountRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val jointAccountMemberRepository: JointAccountMemberRepository,
    private val userRepository: UserRepository,
    private val accountDtoMapper: AccountDtoMapper
) {

    @Transactional(readOnly = true)
    fun execute(authenticatedUserId: UUID): List<AccountResponse> {
        val ownAccounts = accountRepository.findAllByOwnerId(authenticatedUserId)
        val userGroupIds = groupMemberRepository.findAllByUserId(authenticatedUserId)
            .map { it.groupId }
            .toSet()

        val groupIndividualAccounts = if (userGroupIds.isEmpty()) {
            emptyList()
        } else {
            accountRepository.findAllByGroupIdIn(userGroupIds).filter { !it.isJoint }
        }

        val jointMemberships = jointAccountMemberRepository.findAllByUserId(authenticatedUserId)
        val accessibleJointAccounts = if (jointMemberships.isEmpty() || userGroupIds.isEmpty()) {
            emptyList()
        } else {
            accountRepository.findAllByIdIn(jointMemberships.map { it.accountId }.distinct())
                .filter { it.isJoint && it.groupId != null && it.groupId in userGroupIds }
        }

        val allAccessible = (ownAccounts + groupIndividualAccounts + accessibleJointAccounts)
            .distinctBy { it.id }

        val jointIds = allAccessible.filter { it.isJoint }.map { it.id }
        val membersByAccountId = if (jointIds.isEmpty()) {
            emptyMap()
        } else {
            jointAccountMemberRepository.findAllByAccountIdIn(jointIds).groupBy { it.accountId }
        }

        val memberUserIds = membersByAccountId.values.flatten().map { it.userId }.distinct()
        val usersById = if (memberUserIds.isEmpty()) {
            emptyMap()
        } else {
            userRepository.findAllByIdIn(memberUserIds).associateBy { it.id }
        }

        return allAccessible.map { account ->
            val allowedMembers = if (account.isJoint) {
                accountDtoMapper.toSortedUserSummaries(
                    membersByAccountId[account.id].orEmpty().mapNotNull { usersById[it.userId] }
                )
            } else {
                emptyList()
            }
            accountDtoMapper.toAccountResponse(account, allowedMembers)
        }
    }
}
