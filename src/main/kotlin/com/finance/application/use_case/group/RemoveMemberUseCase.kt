package com.finance.application.use_case.group

import com.finance.application.dto.group.GroupResponse
import com.finance.application.mapper.GroupDtoMapper
import com.finance.application.service.GroupService
import com.finance.application.service.UserService
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
import com.finance.domain.repository.JointAccountMemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RemoveMemberUseCase(
    private val groupService: GroupService,
    private val userService: UserService,
    private val groupMemberRepository: GroupMemberRepository,
    private val groupRepository: GroupRepository,
    private val accountRepository: AccountRepository,
    private val jointAccountMemberRepository: JointAccountMemberRepository,
    private val groupDtoMapper: GroupDtoMapper,
) {

    @Transactional
    fun execute(groupId: UUID, targetEmail: String, authenticatedUserId: UUID): GroupResponse {
        val group = groupService.findGroupById(groupId)
        val actor = groupService.findMember(group.id, authenticatedUserId)

        groupService.requireOwnerOrAdmin(actor)

        val target = userService.findByEmail(targetEmail)
        val targetMember = groupService.findMember(group.id, target.id)

        // ADMIN não pode remover o OWNER
        if (targetMember.isOwner()) {
            throw AccessDeniedException("O dono do grupo não pode ser removido. Transfira a titularidade primeiro.")
        }

        // ADMIN não pode remover outro ADMIN (apenas OWNER pode)
        if (actor.isAdmin() && targetMember.isAdmin()) {
            throw AccessDeniedException("Administradores não podem remover outros administradores.")
        }

        // Desvincula todas as contas individuais do usuário que estavam neste grupo
        val groupAccounts = accountRepository.findAllByGroupId(group.id)
        groupAccounts.filter { !it.isJoint && it.ownerId == target.id }.forEach {
            it.unlinkFromGroup()
            accountRepository.save(it)
        }

        // Remove o usuário de todas as contas conjuntas deste grupo
        val jointAccountIds = groupAccounts.filter { it.isJoint }.map { it.id }
        if (jointAccountIds.isNotEmpty()) {
            jointAccountMemberRepository.deleteAllByAccountIdInAndUserId(jointAccountIds, target.id)
        }

        groupMemberRepository.deleteByGroupIdAndUserId(group.id, target.id)

        val updatedGroup = groupRepository.findById(group.id)!!
        return groupDtoMapper.toGroupResponse(updatedGroup, authenticatedUserId)
    }
}
