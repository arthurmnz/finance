package com.finance.application.port

import com.finance.domain.entity.UserEntity
import java.time.Duration

interface TokenProviderPort {
    fun generateAccessToken(user: UserEntity): String
    fun generateRefreshToken(): String
    fun validateAndGetUserEmail(accessToken: String): String?
    fun getAccessTokenExpirationSeconds(): Long
    fun getRefreshTokenDuration(): Duration
}
