package com.finance.application.dto.auth

import com.finance.application.dto.user.UserResponse

data class AuthResult(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: UserResponse,
) {
    fun toAuthResponse(): AuthResponse {
        return AuthResponse(
            accessToken = accessToken,
            tokenType = "Bearer",
            expiresIn = expiresIn,
            user = user,
        )
    }
}
