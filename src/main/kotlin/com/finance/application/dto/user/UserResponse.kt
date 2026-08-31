package com.finance.application.dto.user

import java.time.LocalDateTime
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val email: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
