package com.finance.application.use_case.group

import com.finance.application.dto.group.GroupResponse
import com.finance.application.mapper.GroupDtoMapper
import com.finance.application.service.GroupService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class GetGroupUseCase(
    private val groupService: GroupService,
    private val groupDtoMapper: GroupDtoMapper,
) {

    @Transactional(readOnly = true)
    fun execute(groupId: UUID, authenticatedUserId: UUID): GroupResponse {
        val group = groupService.findGroupById(groupId)

        // Valida que o usuário é membro do grupo
        groupService.findMember(group.id, authenticatedUserId)

        return groupDtoMapper.toGroupResponse(group, authenticatedUserId)
    }
}
