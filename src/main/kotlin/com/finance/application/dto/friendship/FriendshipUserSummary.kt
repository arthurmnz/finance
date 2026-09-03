package com.finance.application.dto.friendship

import java.util.UUID

data class FriendshipUserSummary(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val email: String,
)
