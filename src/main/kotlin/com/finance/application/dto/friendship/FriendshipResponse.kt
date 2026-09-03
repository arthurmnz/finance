package com.finance.application.dto.friendship

import com.finance.domain.enum.FriendshipStatus
import java.time.LocalDateTime
import java.util.UUID

data class FriendshipResponse(
    val id: UUID,
    val requester: FriendshipUserSummary,
    val addressee: FriendshipUserSummary,
    val status: FriendshipStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
