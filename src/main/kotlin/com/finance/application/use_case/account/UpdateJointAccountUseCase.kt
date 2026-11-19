package com.finance.application.use_case.account

import com.finance.application.dto.account.GroupAccountResponse
import com.finance.application.dto.account.UpdateJointAccountRequest
import com.finance.application.mapper.AccountDtoMapper
import com.finance.application.service.GroupService
import com.finance.domain.entity.JointAccountMemberEntity
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.JointAccountMemberRepository
import com.finance.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UpdateJointAccountUseCase(
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
        accountId: UUID,
        request: UpdateJointAccountRequest,
        authenticatedUserId: UUID
    ): GroupAccountResponse {
        groupService.findGroupById(groupId)
        val membership = groupService.findMember(groupId, authenticatedUserId)
        groupService.requireOwnerOrAdmin(membership)

        val account = accountRepository.findById(accountId) ?: throw AccountNotFoundException()
        if (!account.isJoint || account.groupId != groupId) {
            throw AccountNotFoundException("Conta conjunta não encontrada neste grupo.")
        }

        val distinctMemberIds = request.memberIds.distinct()
        if (distinctMemberIds.isEmpty()) {
            throw IllegalArgumentException("A conta conjunta deve possuir ao menos um membro autorizado.")
        }

        distinctMemberIds.forEach { memberId ->
            if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, memberId)) {
                throw IllegalArgumentException("Todos os membros autorizados da conta conjunta devem pertencer ao grupo.")
            }
        }

        account.updateJoint(
            newName = request.name,
            newStartBalance = request.startBalance,
            newBalance = request.balance,
            newType = request.type,
            newTax = request.tax,
            newIncludeInTotal = request.includeInTotal
        )
        val savedAccount = accountRepository.save(account)

        val currentMemberIds = jointAccountMemberRepository.findAllByAccountId(accountId).map { it.userId }.toSet()
        val targetMemberIds = distinctMemberIds.toSet()

        val toRemove = currentMemberIds - targetMemberIds
        val toAdd = targetMemberIds - currentMemberIds

        toRemove.forEach { userId ->
            jointAccountMemberRepository.deleteByAccountIdAndUserId(accountId, userId)
        }
        if (toAdd.isNotEmpty()) {
            jointAccountMemberRepository.saveAll(
                toAdd.map { userId -> JointAccountMemberEntity(accountId = accountId, userId = userId) }
            )
        }

        val allowedMembers = accountDtoMapper.toSortedUserSummaries(userRepository.findAllByIdIn(distinctMemberIds))

        return accountDtoMapper.toGroupAccountResponse(
            account = savedAccount,
            owner = null,
            allowedMembers = allowedMembers
        )
    }
}
