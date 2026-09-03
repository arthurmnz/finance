package com.finance.application.mapper

import com.finance.application.dto.group.GroupInvitationResponse
import com.finance.application.dto.group.GroupMemberResponse
import com.finance.application.dto.group.GroupResponse
import com.finance.application.dto.group.GroupSummaryResponse
import com.finance.application.dto.group.GroupUserSummary
import com.finance.application.service.UserService
import com.finance.domain.entity.GroupEntity
import com.finance.domain.entity.GroupInvitationEntity
import com.finance.domain.entity.GroupMemberEntity
import com.finance.domain.entity.UserEntity
import com.finance.domain.enum.GroupRole
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class GroupDtoMapper(
    private val userService: UserService,
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
) {

    fun toGroupResponse(group: GroupEntity, authenticatedUserId: UUID): GroupResponse {
        val members = groupMemberRepository.findAllByGroupId(group.id)
        val myMember = members.find { it.userId == authenticatedUserId }
        val myRole = myMember?.role ?: GroupRole.MEMBER

        val memberResponses = members.map { member ->
            val user = userService.findById(member.userId)
            toGroupMemberResponse(member, user)
        }

        return GroupResponse(
            id = group.id,
            name = group.name,
            myRole = myRole,
            memberCount = members.size,
            members = memberResponses,
            createdAt = group.createdAt,
            updatedAt = group.updatedAt,
        )
    }

    fun toGroupSummaryResponse(group: GroupEntity, authenticatedUserId: UUID): GroupSummaryResponse {
        val myMember = groupMemberRepository.findByGroupIdAndUserId(group.id, authenticatedUserId)
        val myRole = myMember?.role ?: GroupRole.MEMBER
        val memberCount = groupMemberRepository.countByGroupId(group.id)

        return GroupSummaryResponse(
            id = group.id,
            name = group.name,
            myRole = myRole,
            memberCount = memberCount,
            createdAt = group.createdAt,
        )
    }

    fun toGroupInvitationResponse(invitation: GroupInvitationEntity): GroupInvitationResponse {
        val group = groupRepository.findById(invitation.groupId)
        val inviter = userService.findById(invitation.inviterId)

        return GroupInvitationResponse(
            id = invitation.id,
            group = GroupInvitationResponse.GroupInfo(
                id = invitation.groupId,
                name = group?.name ?: "Grupo removido",
            ),
            inviter = toUserSummary(inviter),
            status = invitation.status,
            createdAt = invitation.createdAt,
            updatedAt = invitation.updatedAt,
        )
    }

    private fun toGroupMemberResponse(member: GroupMemberEntity, user: UserEntity): GroupMemberResponse {
        return GroupMemberResponse(
            user = toUserSummary(user),
            role = member.role,
            joinedAt = member.joinedAt,
        )
    }

    private fun toUserSummary(user: UserEntity): GroupUserSummary {
        return GroupUserSummary(
            id = user.id,
            firstName = user.firstName.value,
            lastName = user.lastName.value,
            email = user.email.normalized,
        )
    }
}
