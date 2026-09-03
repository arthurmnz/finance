package com.finance.application.use_case.group

import com.finance.application.dto.group.GroupResponse
import com.finance.application.dto.group.RenameGroupRequest
import com.finance.application.mapper.GroupDtoMapper
import com.finance.application.service.GroupService
import com.finance.domain.repository.GroupRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RenameGroupUseCase(
    private val groupRepository: GroupRepository,
    private val groupService: GroupService,
    private val groupDtoMapper: GroupDtoMapper,
) {

    @Transactional
    fun execute(groupId: UUID, request: RenameGroupRequest, authenticatedUserId: UUID): GroupResponse {
        val group = groupService.findGroupById(groupId)
        val member = groupService.findMember(group.id, authenticatedUserId)

        groupService.requireCanRename(member)

        group.rename(request.name)
        val saved = groupRepository.save(group)

        return groupDtoMapper.toGroupResponse(saved, authenticatedUserId)
    }
}
