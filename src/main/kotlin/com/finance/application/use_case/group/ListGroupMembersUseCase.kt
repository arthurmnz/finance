package com.finance.application.use_case.group

import com.finance.application.dto.group.GroupMemberResponse
import com.finance.application.dto.group.GroupMembersResponse
import com.finance.application.dto.group.GroupUserSummary
import com.finance.application.service.GroupService
import com.finance.application.service.UserService
import com.finance.domain.repository.GroupMemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ListGroupMembersUseCase(
    private val groupService: GroupService,
    private val groupMemberRepository: GroupMemberRepository,
    private val userService: UserService,
) {

    @Transactional(readOnly = true)
    fun execute(groupId: UUID, authenticatedUserId: UUID): GroupMembersResponse {
        val group = groupService.findGroupById(groupId)

        // Apenas membros do grupo podem listar os membros
        groupService.findMember(group.id, authenticatedUserId)

        val members = groupMemberRepository.findAllByGroupId(group.id).map { member ->
            val user = userService.findById(member.userId)
            GroupMemberResponse(
                user = GroupUserSummary(
                    id = user.id,
                    firstName = user.firstName.value,
                    lastName = user.lastName.value,
                    email = user.email.normalized,
                ),
                role = member.role,
                joinedAt = member.joinedAt,
            )
        }

        return GroupMembersResponse(
            groupId = group.id,
            groupName = group.name,
            members = members,
        )
    }
}
