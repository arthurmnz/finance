package com.finance.application.use_case.account

import com.finance.application.dto.account.GroupAccountResponse
import com.finance.application.mapper.AccountDtoMapper
import com.finance.application.service.GroupService
import com.finance.domain.exception.UserNotFoundException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.JointAccountMemberRepository
import com.finance.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ListGroupAccountsUseCase(
    private val groupService: GroupService,
    private val accountRepository: AccountRepository,
    private val jointAccountMemberRepository: JointAccountMemberRepository,
    private val userRepository: UserRepository,
    private val accountDtoMapper: AccountDtoMapper
) {

    @Transactional(readOnly = true)
    fun execute(groupId: UUID, authenticatedUserId: UUID): List<GroupAccountResponse> {
        groupService.findMember(groupId, authenticatedUserId)

        val allGroupAccounts = accountRepository.findAllByGroupId(groupId)
        if (allGroupAccounts.isEmpty()) return emptyList()

        val jointAccountIds = allGroupAccounts.filter { it.isJoint }.map { it.id }
        val membersByAccountId = if (jointAccountIds.isEmpty()) {
            emptyMap()
        } else {
            jointAccountMemberRepository.findAllByAccountIdIn(jointAccountIds).groupBy { it.accountId }
        }

        val visibleAccounts = allGroupAccounts.filter { account ->
            !account.isJoint || membersByAccountId[account.id]?.any { it.userId == authenticatedUserId } == true
        }
        if (visibleAccounts.isEmpty()) return emptyList()

        val ownerIds = visibleAccounts.mapNotNull { it.ownerId }
        val jointMemberUserIds = visibleAccounts
            .filter { it.isJoint }
            .flatMap { membersByAccountId[it.id].orEmpty().map { m -> m.userId } }
        val allUserIds = (ownerIds + jointMemberUserIds).distinct()
        val usersById = userRepository.findAllByIdIn(allUserIds).associateBy { it.id }

        return visibleAccounts.map { account ->
            if (account.isJoint) {
                val allowedMembers = accountDtoMapper.toSortedUserSummaries(
                    membersByAccountId[account.id].orEmpty().mapNotNull { usersById[it.userId] }
                )
                accountDtoMapper.toGroupAccountResponse(
                    account = account,
                    owner = null,
                    allowedMembers = allowedMembers
                )
            } else {
                val ownerId = account.ownerId!!
                val ownerUser = usersById[ownerId]
                    ?: throw UserNotFoundException("Usuário não encontrado com o ID: $ownerId")
                accountDtoMapper.toGroupAccountResponse(
                    account = account,
                    owner = accountDtoMapper.toUserSummary(ownerUser),
                    allowedMembers = emptyList()
                )
            }
        }
    }
}
