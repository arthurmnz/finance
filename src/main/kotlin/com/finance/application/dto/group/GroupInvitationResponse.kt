package com.finance.application.dto.group

import com.finance.domain.enum.GroupInvitationStatus
import java.time.LocalDateTime
import java.util.UUID

data class GroupInvitationResponse(
    val id: UUID,
    val group: GroupInfo,
    val inviter: GroupUserSummary,
    val status: GroupInvitationStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    data class GroupInfo(
        val id: UUID,
        val name: String,
    )
}
