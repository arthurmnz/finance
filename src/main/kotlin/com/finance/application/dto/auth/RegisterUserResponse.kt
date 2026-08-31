package com.finance.application.dto.auth

import java.time.LocalDateTime
import java.util.UUID

data class RegisterUserResponse(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val email: String,
    val createdAt: LocalDateTime,
)
