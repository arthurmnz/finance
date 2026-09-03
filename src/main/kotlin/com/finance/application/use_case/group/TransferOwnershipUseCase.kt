package com.finance.application.use_case.group

import com.finance.application.dto.group.GroupResponse
import com.finance.application.dto.group.TransferOwnershipRequest
import com.finance.application.mapper.GroupDtoMapper
import com.finance.application.service.GroupService
import com.finance.application.service.UserService
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TransferOwnershipUseCase(
    private val groupService: GroupService,
    private val userService: UserService,
    private val groupMemberRepository: GroupMemberRepository,
    private val groupRepository: GroupRepository,
    private val groupDtoMapper: GroupDtoMapper,
) {

    @Transactional
    fun execute(groupId: UUID, request: TransferOwnershipRequest, authenticatedUserId: UUID): GroupResponse {
        val group = groupService.findGroupById(groupId)
        val currentOwner = groupService.findMember(group.id, authenticatedUserId)

        groupService.requireOwner(currentOwner)

        val newOwnerUser = userService.findByEmail(request.newOwnerEmail)
        require(newOwnerUser.id != authenticatedUserId) { "Você já é o dono do grupo." }

        val newOwnerMember = groupService.findMember(group.id, newOwnerUser.id)

        // Promove o novo dono e rebaixa o antigo para ADMIN
        newOwnerMember.promoteToOwner()
        currentOwner.demoteToAdmin()

        groupMemberRepository.save(newOwnerMember)
        groupMemberRepository.save(currentOwner)

        val updatedGroup = groupRepository.findById(group.id)!!
        return groupDtoMapper.toGroupResponse(updatedGroup, authenticatedUserId)
    }
}
