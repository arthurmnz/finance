package com.finance.application.use_case.account

import com.finance.application.dto.account.CreateJointAccountRequest
import com.finance.application.dto.account.GroupAccountResponse
import com.finance.application.mapper.AccountDtoMapper
import com.finance.application.service.GroupService
import com.finance.domain.entity.AccountEntity
import com.finance.domain.entity.JointAccountMemberEntity
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.JointAccountMemberRepository
import com.finance.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CreateJointAccountUseCase(
    private val groupService: GroupService,
    private val groupMemberRepository: GroupMemberRepository,
    private val accountRepository: AccountRepository,
    private val jointAccountMemberRepository: JointAccountMemberRepository,
    private val userRepository: UserRepository,
    private val accountDtoMapper: AccountDtoMapper
) {

    @Transactional
    fun execute(
        groupId: UUID,
        request: CreateJointAccountRequest,
        authenticatedUserId: UUID
    ): GroupAccountResponse {
        groupService.findGroupById(groupId)
        val membership = groupService.findMember(groupId, authenticatedUserId)
        groupService.requireOwnerOrAdmin(membership)

        val distinctMemberIds = request.memberIds.distinct()
        if (distinctMemberIds.isEmpty()) {
            throw IllegalArgumentException("A conta conjunta deve possuir ao menos um membro autorizado.")
        }

        distinctMemberIds.forEach { memberId ->
            if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, memberId)) {
                throw IllegalArgumentException("Todos os membros autorizados da conta conjunta devem pertencer ao grupo.")
            }
        }

        val jointAccount = AccountEntity.createJoint(
            groupId = groupId,
            name = request.name,
            startBalance = request.startBalance,
            type = request.type,
            tax = request.tax,
            includeInTotal = request.includeInTotal
        )

        val savedAccount = accountRepository.save(jointAccount)
        val memberLinks = distinctMemberIds.map { memberId ->
            JointAccountMemberEntity(accountId = savedAccount.id, userId = memberId)
        }
        jointAccountMemberRepository.saveAll(memberLinks)

        val allowedMembers = accountDtoMapper.toSortedUserSummaries(userRepository.findAllByIdIn(distinctMemberIds))

        return accountDtoMapper.toGroupAccountResponse(
            account = savedAccount,
            owner = null,
            allowedMembers = allowedMembers
        )
    }
}
