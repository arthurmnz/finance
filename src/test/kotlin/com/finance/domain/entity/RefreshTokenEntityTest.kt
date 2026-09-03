package com.finance.domain.entity

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RefreshTokenEntityTest {

    @Test
    fun `should create valid refresh token`() {
        val userId = UUID.randomUUID()
        val expiresAt = LocalDateTime.now().plusDays(7)
        val token = RefreshTokenEntity(
            userId = userId,
            token = "sample_refresh_token_123",
            expiresAt = expiresAt
        )

        assertNotNull(token.id)
        assertEquals(userId, token.userId)
        assertEquals("sample_refresh_token_123", token.token)
        assertTrue(token.isValid)
        assertFalse(token.isRevoked)
        assertFalse(token.isExpired)
        assertNull(token.revokedAt)
    }

    @Test
    fun `should revoke token and become invalid`() {
        val token = RefreshTokenEntity(
            userId = UUID.randomUUID(),
            token = "sample_token",
            expiresAt = LocalDateTime.now().plusDays(7)
        )

        token.revoke()

        assertTrue(token.isRevoked)
        assertFalse(token.isValid)
        assertNotNull(token.revokedAt)
    }

    @Test
    fun `should recognize expired token`() {
        val token = RefreshTokenEntity(
            userId = UUID.randomUUID(),
            token = "expired_token",
            expiresAt = LocalDateTime.now().minusMinutes(1)
        )

        assertTrue(token.isExpired)
        assertFalse(token.isValid)
    }
}
