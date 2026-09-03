package com.finance.application.dto.auth

import com.finance.application.dto.user.UserResponse

data class AuthResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val user: UserResponse,
)
