package com.finance.application.use_case.group

import com.finance.application.dto.group.GroupResponse
import com.finance.application.dto.group.UpdateMemberRoleRequest
import com.finance.application.mapper.GroupDtoMapper
import com.finance.application.service.GroupService
import com.finance.application.service.UserService
import com.finance.domain.enum.GroupRole
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UpdateMemberRoleUseCase(
    private val groupService: GroupService,
    private val userService: UserService,
    private val groupMemberRepository: GroupMemberRepository,
    private val groupRepository: GroupRepository,
    private val groupDtoMapper: GroupDtoMapper,
) {

    @Transactional
    fun execute(groupId: UUID, request: UpdateMemberRoleRequest, authenticatedUserId: UUID): GroupResponse {
        val group = groupService.findGroupById(groupId)
        val actor = groupService.findMember(group.id, authenticatedUserId)

        groupService.requireOwner(actor)

        require(request.role != GroupRole.OWNER) {
            "Para transferir a titularidade, use o endpoint de transferência de ownership."
        }

        val target = userService.findByEmail(request.email)
        require(target.id != authenticatedUserId) { "Você não pode alterar sua própria role." }

        val targetMember = groupService.findMember(group.id, target.id)
        targetMember.updateRole(request.role)
        groupMemberRepository.save(targetMember)

        val updatedGroup = groupRepository.findById(group.id)!!
        return groupDtoMapper.toGroupResponse(updatedGroup, authenticatedUserId)
    }
}
