package com.finance.application.use_case.group

import com.finance.application.dto.group.GroupInvitationResponse
import com.finance.application.dto.group.InviteMemberRequest
import com.finance.application.mapper.GroupDtoMapper
import com.finance.application.service.GroupService
import com.finance.application.service.UserService
import com.finance.domain.entity.GroupInvitationEntity
import com.finance.domain.enum.FriendshipStatus
import com.finance.domain.enum.GroupInvitationStatus
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.GroupFullException
import com.finance.domain.exception.GroupInvitationAlreadyExistsException
import com.finance.domain.repository.FriendshipRepository
import com.finance.domain.repository.GroupInvitationRepository
import com.finance.domain.repository.GroupMemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class InviteMemberUseCase(
    private val groupService: GroupService,
    private val userService: UserService,
    private val friendshipRepository: FriendshipRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val groupInvitationRepository: GroupInvitationRepository,
    private val groupDtoMapper: GroupDtoMapper,
) {

    companion object {
        const val MAX_MEMBERS = 10
    }

    @Transactional
    fun execute(groupId: UUID, request: InviteMemberRequest, authenticatedUserId: UUID): GroupInvitationResponse {
        val group = groupService.findGroupById(groupId)
        val inviter = groupService.findMember(group.id, authenticatedUserId)

        groupService.requireOwnerOrAdmin(inviter)

        // Resolve o convidado pelo e-mail
        val invitee = userService.findByEmail(request.inviteeEmail)

        require(invitee.id != authenticatedUserId) { "Você não pode convidar a si mesmo." }

        // Valida que existe amizade aceita entre quem convida e o convidado
        val friendship = friendshipRepository.findBetweenUsers(authenticatedUserId, invitee.id)
        if (friendship == null || friendship.status != FriendshipStatus.ACCEPTED) {
            throw AccessDeniedException("Você só pode convidar amigos para o grupo.")
        }

        // Valida limite de membros
        val currentMemberCount = groupMemberRepository.countByGroupId(group.id)
        if (currentMemberCount >= MAX_MEMBERS) {
            throw GroupFullException()
        }

        // Valida que o convidado não é membro já
        if (groupMemberRepository.existsByGroupIdAndUserId(group.id, invitee.id)) {
            throw GroupInvitationAlreadyExistsException("Este usuário já é membro do grupo.")
        }

        // Valida que não existe convite pendente
        val existingInvitation = groupInvitationRepository.findByGroupIdAndInviteeIdAndStatus(
            group.id, invitee.id, GroupInvitationStatus.PENDING
        )
        if (existingInvitation != null) {
            throw GroupInvitationAlreadyExistsException()
        }

        val invitation = GroupInvitationEntity(
            groupId = group.id,
            inviterId = authenticatedUserId,
            inviteeId = invitee.id,
        )

        val saved = groupInvitationRepository.save(invitation)
        return groupDtoMapper.toGroupInvitationResponse(saved)
    }
}
