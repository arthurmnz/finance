package com.finance.presentation.util

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class AuthCookieUtil(
    @Value("\${security.cookie.secure:false}")
    private val isSecureCookie: Boolean = false,
) {
    companion object {
        const val REFRESH_TOKEN_COOKIE_NAME = "refreshToken"
        const val REFRESH_TOKEN_PATH = "/api/v1/auth"
    }

    fun createRefreshTokenCookie(token: String, duration: Duration): ResponseCookie {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, token)
            .httpOnly(true)
            .secure(isSecureCookie)
            .path(REFRESH_TOKEN_PATH)
            .maxAge(duration)
            .sameSite("Strict")
            .build()
    }

    fun createEmptyRefreshTokenCookie(): ResponseCookie {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(isSecureCookie)
            .path(REFRESH_TOKEN_PATH)
            .maxAge(0)
            .sameSite("Strict")
            .build()
    }
}
