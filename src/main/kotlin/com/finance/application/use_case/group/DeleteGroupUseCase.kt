package com.finance.application.use_case.group

import com.finance.application.service.GroupService
import com.finance.domain.repository.GroupInvitationRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DeleteGroupUseCase(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val groupInvitationRepository: GroupInvitationRepository,
    private val groupService: GroupService,
) {

    @Transactional
    fun execute(groupId: UUID, authenticatedUserId: UUID) {
        val group = groupService.findGroupById(groupId)
        val member = groupService.findMember(group.id, authenticatedUserId)

        groupService.requireOwner(member)

        // Deleta convites e membros antes do grupo (integridade referencial)
        groupInvitationRepository.deleteAllByGroupId(group.id)
        groupMemberRepository.deleteAllByGroupId(group.id)
        groupRepository.deleteById(group.id)
    }
}
