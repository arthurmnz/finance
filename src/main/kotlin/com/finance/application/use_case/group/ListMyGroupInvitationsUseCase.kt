package com.finance.application.use_case.group

import com.finance.application.dto.group.GroupInvitationResponse
import com.finance.application.mapper.GroupDtoMapper
import com.finance.domain.enum.GroupInvitationStatus
import com.finance.domain.repository.GroupInvitationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ListMyGroupInvitationsUseCase(
    private val groupInvitationRepository: GroupInvitationRepository,
    private val groupDtoMapper: GroupDtoMapper,
) {

    @Transactional(readOnly = true)
    fun execute(authenticatedUserId: UUID): List<GroupInvitationResponse> {
        return groupInvitationRepository
            .findAllByInviteeIdAndStatus(authenticatedUserId, GroupInvitationStatus.PENDING)
            .map { groupDtoMapper.toGroupInvitationResponse(it) }
    }
}
