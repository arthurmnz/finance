package com.finance.application.dto.user

import com.finance.domain.enum.UserRole
import java.time.LocalDateTime
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: UserRole = UserRole.USER,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
