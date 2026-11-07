package com.finance.application.use_case.auth

import com.finance.application.dto.auth.ResetPasswordRequest
import com.finance.application.port.PasswordEncoderPort
import com.finance.application.service.InMemoryUserRepository
import com.finance.domain.entity.PasswordRecoveryTokenEntity
import com.finance.domain.entity.RefreshTokenEntity
import com.finance.domain.entity.UserEntity
import com.finance.domain.repository.RefreshTokenRepository
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ResetPasswordUseCaseTest {

    private lateinit var userRepository: InMemoryUserRepository
    private lateinit var tokenRepository: InMemoryPasswordRecoveryTokenRepository
    private lateinit var refreshTokenRepository: InMemoryResetRefreshTokenRepository
    private lateinit var passwordEncoder: FakePasswordEncoder
    private lateinit var useCase: ResetPasswordUseCase
    private lateinit var user: UserEntity

    @BeforeEach
    fun setup() {
        userRepository = InMemoryUserRepository()
        tokenRepository = InMemoryPasswordRecoveryTokenRepository()
        refreshTokenRepository = InMemoryResetRefreshTokenRepository()
        passwordEncoder = FakePasswordEncoder()

        useCase = ResetPasswordUseCase(
            passwordRecoveryTokenRepository = tokenRepository,
            userRepository = userRepository,
            refreshTokenRepository = refreshTokenRepository,
            passwordEncoder = passwordEncoder,
        )

        user = UserEntity(
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur@finance.com"),
            passwordHash = PasswordHash("oldHash123!"),
        )
        userRepository.save(user)
    }

    @Test
    fun `should successfully reset password, mark token as used and revoke refresh tokens`() {
        val rawToken = "my-valid-secure-token-123456789"
        val tokenHash = sha256(rawToken)

        val tokenEntity = PasswordRecoveryTokenEntity(
            userId = user.id,
            tokenHash = tokenHash,
            expiresAt = LocalDateTime.now().plusMinutes(30),
        )
        tokenRepository.save(tokenEntity)

        // Sessão ativa
        val refreshToken = RefreshTokenEntity(
            userId = user.id,
            token = "active-refresh-token",
            expiresAt = LocalDateTime.now().plusDays(7),
        )
        refreshTokenRepository.save(refreshToken)
        assertFalse(refreshToken.isRevoked)

        val response = useCase.execute(
            ResetPasswordRequest(
                token = rawToken,
                newPassword = "NewPassword123!",
            )
        )

        assertEquals(ResetPasswordUseCase.SUCCESS_MESSAGE, response.message)

        // Verifica token marcado como usado
        val updatedToken = tokenRepository.findByTokenHash(tokenHash)
        assertNotNull(updatedToken)
        assertTrue(updatedToken.isUsed)
        assertFalse(updatedToken.isValid)

        // Verifica senha do usuário atualizada
        val updatedUser = userRepository.findById(user.id)
        assertNotNull(updatedUser)
        assertEquals("hashed_NewPassword123!", updatedUser.passwordHash.value)

        // Verifica refresh tokens revogados
        val updatedRefreshToken = refreshTokenRepository.findByToken("active-refresh-token")
        assertNotNull(updatedRefreshToken)
        assertTrue(updatedRefreshToken.isRevoked)
    }

    @Test
    fun `should throw IllegalArgumentException when token is expired`() {
        val rawToken = "expired-token"
        val tokenHash = sha256(rawToken)

        tokenRepository.save(
            PasswordRecoveryTokenEntity(
                userId = user.id,
                tokenHash = tokenHash,
                expiresAt = LocalDateTime.now().minusMinutes(1),
            )
        )

        val ex = assertThrows<IllegalArgumentException> {
            useCase.execute(ResetPasswordRequest(token = rawToken, newPassword = "NewPassword123!"))
        }
        assertEquals(ResetPasswordUseCase.INVALID_TOKEN_MESSAGE, ex.message)
    }

    @Test
    fun `should throw IllegalArgumentException when token is already used`() {
        val rawToken = "used-token"
        val tokenHash = sha256(rawToken)

        val tokenEntity = PasswordRecoveryTokenEntity(
            userId = user.id,
            tokenHash = tokenHash,
            expiresAt = LocalDateTime.now().plusMinutes(30),
        )
        tokenEntity.markAsUsed()
        tokenRepository.save(tokenEntity)

        val ex = assertThrows<IllegalArgumentException> {
            useCase.execute(ResetPasswordRequest(token = rawToken, newPassword = "NewPassword123!"))
        }
        assertEquals(ResetPasswordUseCase.INVALID_TOKEN_MESSAGE, ex.message)
    }

    @Test
    fun `should throw IllegalArgumentException when token is already revoked`() {
        val rawToken = "revoked-token"
        val tokenHash = sha256(rawToken)

        val tokenEntity = PasswordRecoveryTokenEntity(
            userId = user.id,
            tokenHash = tokenHash,
            expiresAt = LocalDateTime.now().plusMinutes(30),
        )
        tokenEntity.revoke()
        tokenRepository.save(tokenEntity)

        val ex = assertThrows<IllegalArgumentException> {
            useCase.execute(ResetPasswordRequest(token = rawToken, newPassword = "NewPassword123!"))
        }
        assertEquals(ResetPasswordUseCase.INVALID_TOKEN_MESSAGE, ex.message)
    }

    @Test
    fun `should throw IllegalArgumentException when token is blank or not found`() {
        assertThrows<IllegalArgumentException> {
            useCase.execute(ResetPasswordRequest(token = "", newPassword = "NewPassword123!"))
        }
        assertThrows<IllegalArgumentException> {
            useCase.execute(ResetPasswordRequest(token = "non-existent-token", newPassword = "NewPassword123!"))
        }
    }

    @Test
    fun `should throw IllegalArgumentException when new password does not meet password rules`() {
        val rawToken = "token-test"
        tokenRepository.save(
            PasswordRecoveryTokenEntity(
                userId = user.id,
                tokenHash = sha256(rawToken),
                expiresAt = LocalDateTime.now().plusMinutes(30),
            )
        )

        val ex = assertThrows<IllegalArgumentException> {
            useCase.execute(ResetPasswordRequest(token = rawToken, newPassword = "weak"))
        }
        assertTrue(ex.message!!.contains("mínimo 8 caracteres"))
    }

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

class InMemoryResetRefreshTokenRepository : RefreshTokenRepository {
    val tokens = mutableMapOf<String, RefreshTokenEntity>()

    override fun save(refreshToken: RefreshTokenEntity): RefreshTokenEntity {
        tokens[refreshToken.token] = refreshToken
        return refreshToken
    }

    override fun findByToken(token: String): RefreshTokenEntity? = tokens[token]

    override fun revokeByToken(token: String) {
        tokens[token]?.revoke()
    }

    override fun revokeAllByUserId(userId: UUID) {
        tokens.values.filter { it.userId == userId }.forEach { it.revoke() }
    }

    override fun deleteByToken(token: String) {
        tokens.remove(token)
    }
}
