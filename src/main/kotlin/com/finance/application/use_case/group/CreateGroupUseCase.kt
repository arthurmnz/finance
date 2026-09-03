package com.finance.application.use_case.group

import com.finance.application.dto.group.CreateGroupRequest
import com.finance.application.dto.group.GroupResponse
import com.finance.application.mapper.GroupDtoMapper
import com.finance.domain.entity.GroupEntity
import com.finance.domain.entity.GroupMemberEntity
import com.finance.domain.enum.GroupRole
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CreateGroupUseCase(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val groupDtoMapper: GroupDtoMapper,
) {

    @Transactional
    fun execute(request: CreateGroupRequest, authenticatedUserId: UUID): GroupResponse {
        val group = GroupEntity(name = request.name)
        val savedGroup = groupRepository.save(group)

        // O criador entra automaticamente como OWNER
        val owner = GroupMemberEntity(
            groupId = savedGroup.id,
            userId = authenticatedUserId,
            role = GroupRole.OWNER,
        )
        groupMemberRepository.save(owner)

        return groupDtoMapper.toGroupResponse(savedGroup, authenticatedUserId)
    }
}
