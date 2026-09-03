package com.finance.application.use_case.group

import com.finance.application.dto.group.GroupSummaryResponse
import com.finance.application.mapper.GroupDtoMapper
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ListMyGroupsUseCase(
    private val groupMemberRepository: GroupMemberRepository,
    private val groupRepository: GroupRepository,
    private val groupDtoMapper: GroupDtoMapper,
) {

    @Transactional(readOnly = true)
    fun execute(authenticatedUserId: UUID): List<GroupSummaryResponse> {
        return groupMemberRepository
            .findAllByUserId(authenticatedUserId)
            .mapNotNull { membership ->
                val group = groupRepository.findById(membership.groupId) ?: return@mapNotNull null
                groupDtoMapper.toGroupSummaryResponse(group, authenticatedUserId)
            }
    }
}
