package com.finance.application.use_case.group

import com.finance.application.dto.group.GroupResponse
import com.finance.application.mapper.GroupDtoMapper
import com.finance.application.service.GroupService
import com.finance.domain.entity.GroupMemberEntity
import com.finance.domain.enum.GroupInvitationStatus
import com.finance.domain.enum.GroupRole
import com.finance.domain.exception.GroupInvitationNotFoundException
import com.finance.domain.repository.GroupInvitationRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AcceptGroupInvitationUseCase(
    private val groupInvitationRepository: GroupInvitationRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val groupRepository: GroupRepository,
    private val groupService: GroupService,
    private val groupDtoMapper: GroupDtoMapper,
) {

    @Transactional
    fun execute(invitationId: UUID, authenticatedUserId: UUID): GroupResponse {
        val invitation = groupInvitationRepository.findById(invitationId)
            ?: throw GroupInvitationNotFoundException()

        require(invitation.inviteeId == authenticatedUserId) {
            "Você não pode aceitar um convite que não é seu."
        }

        val group = groupService.findGroupById(invitation.groupId)

        invitation.accept()
        groupInvitationRepository.save(invitation)

        // Adiciona o usuário como MEMBER do grupo
        val newMember = GroupMemberEntity(
            groupId = group.id,
            userId = authenticatedUserId,
            role = GroupRole.MEMBER,
        )
        groupMemberRepository.save(newMember)

        return groupDtoMapper.toGroupResponse(group, authenticatedUserId)
    }
}
