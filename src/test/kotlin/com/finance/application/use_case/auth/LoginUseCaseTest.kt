package com.finance.application.use_case.auth

import com.finance.application.dto.auth.LoginRequest
import com.finance.application.mapper.UserDtoMapper
import com.finance.application.port.TokenProviderPort
import com.finance.application.service.InMemoryUserRepository
import com.finance.application.service.UserService
import com.finance.domain.entity.RefreshTokenEntity
import com.finance.domain.entity.UserEntity
import com.finance.domain.exception.UnauthorizedException
import com.finance.domain.repository.RefreshTokenRepository
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LoginUseCaseTest {

    private lateinit var userRepository: InMemoryUserRepository
    private lateinit var refreshTokenRepository: InMemoryRefreshTokenRepository
    private lateinit var userService: UserService
    private lateinit var passwordEncoder: FakePasswordEncoder
    private lateinit var tokenProvider: FakeTokenProvider
    private lateinit var userDtoMapper: UserDtoMapper
    private lateinit var loginUseCase: LoginUseCase

    @BeforeEach
    fun setup() {
        userRepository = InMemoryUserRepository()
        refreshTokenRepository = InMemoryRefreshTokenRepository()
        userService = UserService(userRepository)
        passwordEncoder = FakePasswordEncoder()
        tokenProvider = FakeTokenProvider()
        userDtoMapper = UserDtoMapper()

        loginUseCase = LoginUseCase(
            userService = userService,
            passwordEncoder = passwordEncoder,
            tokenProvider = tokenProvider,
            refreshTokenRepository = refreshTokenRepository,
            userDtoMapper = userDtoMapper
        )
    }

    @Test
    fun `should login successfully and return access and refresh token`() {
        val user = UserEntity(
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur@finance.com"),
            passwordHash = PasswordHash("hashed_Secret123")
        )
        userRepository.save(user)

        val request = LoginRequest(email = "arthur@finance.com", password = "Secret123")
        val result = loginUseCase.execute(request)

        assertNotNull(result.accessToken)
        assertNotNull(result.refreshToken)
        assertEquals("Arthur", result.user.firstName)
        assertEquals("arthur@finance.com", result.user.email)

        val savedToken = refreshTokenRepository.findByToken(result.refreshToken)
        assertNotNull(savedToken)
        assertEquals(user.id, savedToken.userId)
        assertTrue(savedToken.isValid)
    }

    @Test
    fun `should throw UnauthorizedException when email not found`() {
        val request = LoginRequest(email = "nonexistent@finance.com", password = "Secret123")

        assertThrows<UnauthorizedException> {
            loginUseCase.execute(request)
        }
    }

    @Test
    fun `should throw UnauthorizedException when password does not match`() {
        val user = UserEntity(
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur@finance.com"),
            passwordHash = PasswordHash("hashed_Secret123")
        )
        userRepository.save(user)

        val request = LoginRequest(email = "arthur@finance.com", password = "WrongPassword")

        assertThrows<UnauthorizedException> {
            loginUseCase.execute(request)
        }
    }
}

class InMemoryRefreshTokenRepository : RefreshTokenRepository {
    private val storage = mutableMapOf<String, RefreshTokenEntity>()

    override fun save(refreshToken: RefreshTokenEntity): RefreshTokenEntity {
        storage[refreshToken.token] = refreshToken
        return refreshToken
    }

    override fun findByToken(token: String): RefreshTokenEntity? = storage[token]

    override fun revokeByToken(token: String) {
        storage[token]?.revoke()
    }

    override fun revokeAllByUserId(userId: UUID) {
        storage.values.filter { it.userId == userId }.forEach { it.revoke() }
    }

    override fun deleteByToken(token: String) {
        storage.remove(token)
    }
}

class FakeTokenProvider : TokenProviderPort {
    override fun generateAccessToken(user: UserEntity): String = "jwt_access_token_for_${user.email.normalized}"
    override fun generateRefreshToken(): String = "refresh_token_${UUID.randomUUID()}"
    override fun validateAndGetUserEmail(accessToken: String): String? = null
    override fun getAccessTokenExpirationSeconds(): Long = 900
    override fun getRefreshTokenDuration(): Duration = Duration.ofDays(7)
}
