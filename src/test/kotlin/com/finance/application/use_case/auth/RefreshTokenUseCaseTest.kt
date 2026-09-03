package com.finance.application.use_case.auth

import com.finance.application.mapper.UserDtoMapper
import com.finance.application.service.InMemoryUserRepository
import com.finance.application.service.UserService
import com.finance.domain.entity.RefreshTokenEntity
import com.finance.domain.entity.UserEntity
import com.finance.domain.exception.UnauthorizedException
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RefreshTokenUseCaseTest {

    private lateinit var userRepository: InMemoryUserRepository
    private lateinit var refreshTokenRepository: InMemoryRefreshTokenRepository
    private lateinit var userService: UserService
    private lateinit var tokenProvider: FakeTokenProvider
    private lateinit var userDtoMapper: UserDtoMapper
    private lateinit var refreshTokenUseCase: RefreshTokenUseCase
    private lateinit var user: UserEntity

    @BeforeEach
    fun setup() {
        userRepository = InMemoryUserRepository()
        refreshTokenRepository = InMemoryRefreshTokenRepository()
        userService = UserService(userRepository)
        tokenProvider = FakeTokenProvider()
        userDtoMapper = UserDtoMapper()

        refreshTokenUseCase = RefreshTokenUseCase(
            refreshTokenRepository = refreshTokenRepository,
            userService = userService,
            tokenProvider = tokenProvider,
            userDtoMapper = userDtoMapper
        )

        user = UserEntity(
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur@finance.com"),
            passwordHash = PasswordHash("hashed123")
        )
        userRepository.save(user)
    }

    @Test
    fun `should rotate refresh token successfully`() {
        val oldToken = "initial_refresh_token_123"
        refreshTokenRepository.save(
            RefreshTokenEntity(
                userId = user.id,
                token = oldToken,
                expiresAt = LocalDateTime.now().plusDays(7)
            )
        )

        val result = refreshTokenUseCase.execute(oldToken)

        assertNotNull(result.accessToken)
        assertNotNull(result.refreshToken)
        assertNotEquals(oldToken, result.refreshToken)

        val oldTokenInDb = refreshTokenRepository.findByToken(oldToken)
        assertNotNull(oldTokenInDb)
        assertTrue(oldTokenInDb.isRevoked)

        val newTokenInDb = refreshTokenRepository.findByToken(result.refreshToken)
        assertNotNull(newTokenInDb)
        assertTrue(newTokenInDb.isValid)
    }

    @Test
    fun `should revoke all user tokens when reused token is presented`() {
        val revokedToken = "already_revoked_token"
        val anotherValidToken = "valid_token_for_same_user"

        val revokedEntity = RefreshTokenEntity(
            userId = user.id,
            token = revokedToken,
            expiresAt = LocalDateTime.now().plusDays(7)
        )
        revokedEntity.revoke()
        refreshTokenRepository.save(revokedEntity)

        refreshTokenRepository.save(
            RefreshTokenEntity(
                userId = user.id,
                token = anotherValidToken,
                expiresAt = LocalDateTime.now().plusDays(7)
            )
        )

        assertThrows<UnauthorizedException> {
            refreshTokenUseCase.execute(revokedToken)
        }

        val anotherTokenInDb = refreshTokenRepository.findByToken(anotherValidToken)
        assertNotNull(anotherTokenInDb)
        assertTrue(anotherTokenInDb.isRevoked)
    }

    @Test
    fun `should throw UnauthorizedException when refresh token is expired`() {
        val expiredToken = "expired_token_123"
        refreshTokenRepository.save(
            RefreshTokenEntity(
                userId = user.id,
                token = expiredToken,
                expiresAt = LocalDateTime.now().minusMinutes(5)
            )
        )

        assertThrows<UnauthorizedException> {
            refreshTokenUseCase.execute(expiredToken)
        }
    }

    @Test
    fun `should throw UnauthorizedException when refresh token is null or blank`() {
        assertThrows<UnauthorizedException> {
            refreshTokenUseCase.execute(null)
        }
        assertThrows<UnauthorizedException> {
            refreshTokenUseCase.execute("   ")
        }
    }
}
