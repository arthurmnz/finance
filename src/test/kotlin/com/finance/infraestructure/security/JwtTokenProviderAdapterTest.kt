package com.finance.infraestructure.security

import com.finance.domain.entity.UserEntity
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JwtTokenProviderAdapterTest {

    private val jwtProvider = JwtTokenProviderAdapter(
        jwtSecret = "very_secret_key_for_testing_purposes_at_least_256_bits_12345",
        accessTokenExpirationMinutes = 15,
        refreshTokenExpirationDays = 7,
    )

    @Test
    fun `should generate and validate access token with email as subject`() {
        val user = UserEntity(
            id = UUID.randomUUID(),
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur@finance.com"),
            passwordHash = PasswordHash("hashed123")
        )

        val token = jwtProvider.generateAccessToken(user)
        assertNotNull(token)
        assertTrue(token.isNotEmpty())

        val extractedEmail = jwtProvider.validateAndGetUserEmail(token)
        assertEquals(user.email.normalized, extractedEmail)
    }

    @Test
    fun `should generate secure random refresh token`() {
        val token1 = jwtProvider.generateRefreshToken()
        val token2 = jwtProvider.generateRefreshToken()

        assertNotNull(token1)
        assertNotNull(token2)
        assertTrue(token1 != token2)
        assertTrue(token1.length >= 32)
    }

    @Test
    fun `should return null for invalid access token`() {
        val invalidToken = "invalid.jwt.token"
        val email = jwtProvider.validateAndGetUserEmail(invalidToken)
        assertNull(email)
    }
}
