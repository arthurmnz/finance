package com.finance.application.use_case.group

import com.finance.application.dto.group.GroupInvitationResponse
import com.finance.application.mapper.GroupDtoMapper
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.GroupInvitationNotFoundException
import com.finance.domain.repository.GroupInvitationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RejectGroupInvitationUseCase(
    private val groupInvitationRepository: GroupInvitationRepository,
    private val groupDtoMapper: GroupDtoMapper,
) {

    @Transactional
    fun execute(invitationId: UUID, authenticatedUserId: UUID): GroupInvitationResponse {
        val invitation = groupInvitationRepository.findById(invitationId)
            ?: throw GroupInvitationNotFoundException()

        if (invitation.inviteeId != authenticatedUserId) {
            throw AccessDeniedException("Você não pode recusar um convite que não é seu.")
        }

        invitation.reject()
        val saved = groupInvitationRepository.save(invitation)

        return groupDtoMapper.toGroupInvitationResponse(saved)
    }
}
