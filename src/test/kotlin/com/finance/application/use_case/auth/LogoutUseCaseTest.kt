package com.finance.application.use_case.auth

import com.finance.domain.entity.RefreshTokenEntity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LogoutUseCaseTest {

    private lateinit var refreshTokenRepository: InMemoryRefreshTokenRepository
    private lateinit var logoutUseCase: LogoutUseCase

    @BeforeEach
    fun setup() {
        refreshTokenRepository = InMemoryRefreshTokenRepository()
        logoutUseCase = LogoutUseCase(refreshTokenRepository)
    }

    @Test
    fun `should revoke refresh token on logout`() {
        val token = "active_token_123"
        refreshTokenRepository.save(
            RefreshTokenEntity(
                userId = UUID.randomUUID(),
                token = token,
                expiresAt = LocalDateTime.now().plusDays(7)
            )
        )

        logoutUseCase.execute(token)

        val revoked = refreshTokenRepository.findByToken(token)
        assertNotNull(revoked)
        assertTrue(revoked.isRevoked)
    }

    @Test
    fun `should handle null or blank token gracefully on logout`() {
        logoutUseCase.execute(null)
        logoutUseCase.execute("   ")
    }
}
