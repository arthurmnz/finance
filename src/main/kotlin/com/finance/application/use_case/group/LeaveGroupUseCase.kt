package com.finance.application.use_case.group

import com.finance.application.service.GroupService
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.AccountHasLinkedGroupException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.JointAccountMemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class LeaveGroupUseCase(
    private val groupService: GroupService,
    private val groupMemberRepository: GroupMemberRepository,
    private val accountRepository: AccountRepository,
    private val jointAccountMemberRepository: JointAccountMemberRepository
) {

    @Transactional
    fun execute(groupId: UUID, authenticatedUserId: UUID) {
        val group = groupService.findGroupById(groupId)
        val member = groupService.findMember(group.id, authenticatedUserId)

        if (member.isOwner()) {
            throw AccessDeniedException(
                "O dono não pode sair do grupo. Transfira a titularidade antes de sair."
            )
        }

        // Valida que o usuário não tem contas individuais vinculadas a este grupo antes de permitir a saída.
        if (accountRepository.existsByOwnerIdAndGroupId(authenticatedUserId, groupId)) {
            throw AccountHasLinkedGroupException("Você possui contas vinculadas a este grupo. Desvincule-as antes de sair.")
        }

        val jointAccountIds = accountRepository.findAllByGroupIdAndIsJointTrue(groupId).map { it.id }
        if (jointAccountIds.isNotEmpty()) {
            jointAccountMemberRepository.deleteAllByAccountIdInAndUserId(jointAccountIds, authenticatedUserId)
        }

        groupMemberRepository.deleteByGroupIdAndUserId(group.id, authenticatedUserId)
    }
}
