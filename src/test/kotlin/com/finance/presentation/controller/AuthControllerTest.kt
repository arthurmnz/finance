package com.finance.presentation.controller

import com.finance.application.dto.auth.LoginRequest
import com.finance.application.dto.auth.RegisterUserRequest
import com.finance.application.mapper.UserDtoMapper
import com.finance.application.service.InMemoryUserRepository
import com.finance.application.service.UserService
import com.finance.application.use_case.auth.FakeEmailSender
import com.finance.application.use_case.auth.FakeEventPublisher
import com.finance.application.use_case.auth.FakePasswordEncoder
import com.finance.application.use_case.auth.FakeTokenProvider
import com.finance.application.use_case.auth.ForgotPasswordUseCase
import com.finance.application.use_case.auth.InMemoryPasswordRecoveryTokenRepository
import com.finance.application.use_case.auth.InMemoryRefreshTokenRepository
import com.finance.application.use_case.auth.LoginUseCase
import com.finance.application.use_case.auth.LogoutUseCase
import com.finance.application.use_case.auth.RefreshTokenUseCase
import com.finance.application.use_case.auth.RegisterUserUseCase
import com.finance.application.use_case.auth.ResetPasswordUseCase
import com.finance.domain.entity.PasswordRecoveryTokenEntity
import com.finance.domain.entity.RefreshTokenEntity
import com.finance.domain.entity.UserEntity
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import com.finance.infraestructure.security.RateLimiterService
import com.finance.presentation.exception.GlobalExceptionHandler
import com.finance.presentation.util.AuthCookieUtil
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.UUID

class AuthControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var userRepository: InMemoryUserRepository
    private lateinit var refreshTokenRepository: InMemoryRefreshTokenRepository
    private lateinit var passwordRecoveryTokenRepository: InMemoryPasswordRecoveryTokenRepository
    private lateinit var fakeEmailSender: FakeEmailSender
    private lateinit var rateLimiterService: RateLimiterService
    private lateinit var userService: UserService
    private lateinit var passwordEncoder: FakePasswordEncoder
    private lateinit var tokenProvider: FakeTokenProvider
    private lateinit var userDtoMapper: UserDtoMapper
    private lateinit var authCookieUtil: AuthCookieUtil
    private lateinit var forgotPasswordUseCase: ForgotPasswordUseCase
    private lateinit var resetPasswordUseCase: ResetPasswordUseCase

    @BeforeEach
    fun setup() {
        userRepository = InMemoryUserRepository()
        refreshTokenRepository = InMemoryRefreshTokenRepository()
        passwordRecoveryTokenRepository = InMemoryPasswordRecoveryTokenRepository()
        fakeEmailSender = FakeEmailSender()
        rateLimiterService = RateLimiterService()
        userService = UserService(userRepository)
        passwordEncoder = FakePasswordEncoder()
        tokenProvider = FakeTokenProvider()
        userDtoMapper = UserDtoMapper()
        authCookieUtil = AuthCookieUtil(isSecureCookie = false)

        val registerUserUseCase = RegisterUserUseCase(
            userRepository = userRepository,
            userService = userService,
            passwordEncoder = passwordEncoder,
            userDtoMapper = userDtoMapper
        )

        val loginUseCase = LoginUseCase(
            userService = userService,
            passwordEncoder = passwordEncoder,
            tokenProvider = tokenProvider,
            refreshTokenRepository = refreshTokenRepository,
            userDtoMapper = userDtoMapper
        )

        val refreshTokenUseCase = RefreshTokenUseCase(
            refreshTokenRepository = refreshTokenRepository,
            userService = userService,
            tokenProvider = tokenProvider,
            userDtoMapper = userDtoMapper
        )

        val logoutUseCase = LogoutUseCase(refreshTokenRepository)

        forgotPasswordUseCase = ForgotPasswordUseCase(
            userRepository = userRepository,
            passwordRecoveryTokenRepository = passwordRecoveryTokenRepository,
            eventPublisher = FakeEventPublisher(),
            rateLimiterService = rateLimiterService,
            frontendUrl = "http://localhost:5173",
        )

        resetPasswordUseCase = ResetPasswordUseCase(
            passwordRecoveryTokenRepository = passwordRecoveryTokenRepository,
            userRepository = userRepository,
            refreshTokenRepository = refreshTokenRepository,
            passwordEncoder = passwordEncoder,
        )

        val authController = AuthController(
            registerUserUseCase = registerUserUseCase,
            loginUseCase = loginUseCase,
            refreshTokenUseCase = refreshTokenUseCase,
            logoutUseCase = logoutUseCase,
            forgotPasswordUseCase = forgotPasswordUseCase,
            resetPasswordUseCase = resetPasswordUseCase,
            rateLimiterService = rateLimiterService,
            tokenProvider = tokenProvider,
            authCookieUtil = authCookieUtil
        )

        mockMvc = MockMvcBuilders
            .standaloneSetup(authController)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `should register a new user with status 201 Created`() {
        val json = """
            {
                "firstName": "Arthur",
                "lastName": "Menezes",
                "email": "arthur.auth@finance.com",
                "password": "SecretPassword123!"
            }
        """.trimIndent()

        mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { exists() }
            jsonPath("$.firstName") { value("Arthur") }
            jsonPath("$.lastName") { value("Menezes") }
            jsonPath("$.email") { value("arthur.auth@finance.com") }
        }
    }

    @Test
    fun `should login and set HttpOnly refresh token cookie with 200 OK`() {
        val user = UserEntity(
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur.login@finance.com"),
            passwordHash = PasswordHash("hashed_SecretPassword123")
        )
        userRepository.save(user)

        val json = """
            {
                "email": "arthur.login@finance.com",
                "password": "SecretPassword123"
            }
        """.trimIndent()

        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isOk() }
            cookie {
                exists("refreshToken")
                httpOnly("refreshToken", true)
            }
            jsonPath("$.accessToken") { exists() }
            jsonPath("$.tokenType") { value("Bearer") }
            jsonPath("$.user.email") { value("arthur.login@finance.com") }
        }
    }

    @Test
    fun `should refresh tokens using cookie and rotate refresh token with 200 OK`() {
        val user = UserEntity(
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur.refresh@finance.com"),
            passwordHash = PasswordHash("hashed_Secret123")
        )
        userRepository.save(user)

        val initialToken = "my_initial_refresh_token"
        refreshTokenRepository.save(
            RefreshTokenEntity(
                userId = user.id,
                token = initialToken,
                expiresAt = LocalDateTime.now().plusDays(7)
            )
        )

        mockMvc.post("/api/v1/auth/refresh") {
            cookie(Cookie("refreshToken", initialToken))
        }.andExpect {
            status { isOk() }
            cookie {
                exists("refreshToken")
                httpOnly("refreshToken", true)
            }
            jsonPath("$.accessToken") { exists() }
        }
    }

    @Test
    fun `should logout and clear refresh token cookie with 204 No Content`() {
        val initialToken = "token_to_logout"
        refreshTokenRepository.save(
            RefreshTokenEntity(
                userId = UUID.randomUUID(),
                token = initialToken,
                expiresAt = LocalDateTime.now().plusDays(7)
            )
        )

        mockMvc.post("/api/v1/auth/logout") {
            cookie(Cookie("refreshToken", initialToken))
        }.andExpect {
            status { isNoContent() }
            cookie {
                exists("refreshToken")
                maxAge("refreshToken", 0)
            }
        }
    }

    @Test
    fun `should request forgot password and return 200 OK`() {
        val user = UserEntity(
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur.forgot@finance.com"),
            passwordHash = PasswordHash("hashed123"),
        )
        userRepository.save(user)

        val json = """{"email": "arthur.forgot@finance.com"}"""

        mockMvc.post("/api/v1/auth/forgot-password") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isOk() }
            jsonPath("$.message") { value(ForgotPasswordUseCase.SUCCESS_MESSAGE) }
        }
    }

    @Test
    fun `should reset password successfully and return 200 OK`() {
        val user = UserEntity(
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur.reset@finance.com"),
            passwordHash = PasswordHash("oldHashed123"),
        )
        userRepository.save(user)

        val rawToken = "valid-reset-token-sample"
        val md = MessageDigest.getInstance("SHA-256")
        val tokenHash = md.digest(rawToken.toByteArray()).joinToString("") { "%02x".format(it) }

        passwordRecoveryTokenRepository.save(
            PasswordRecoveryTokenEntity(
                userId = user.id,
                tokenHash = tokenHash,
                expiresAt = LocalDateTime.now().plusMinutes(30),
            )
        )

        val json = """
            {
                "token": "$rawToken",
                "newPassword": "BrandNewPassword123!"
            }
        """.trimIndent()

        mockMvc.post("/api/v1/auth/reset-password") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isOk() }
            jsonPath("$.message") { value(ResetPasswordUseCase.SUCCESS_MESSAGE) }
        }
    }

    @Test
    fun `should enforce rate limit returning 429 Too Many Requests on the 6th call`() {
        val json = """{"email": "rate.limit@finance.com"}"""

        // 5 chamadas com sucesso
        repeat(5) {
            mockMvc.post("/api/v1/auth/forgot-password") {
                contentType = MediaType.APPLICATION_JSON
                content = json
                with { it.remoteAddr = "198.51.100.1"; it }
            }.andExpect {
                status { isOk() }
            }
        }

        // 6ª chamada deve receber 429
        mockMvc.post("/api/v1/auth/forgot-password") {
            contentType = MediaType.APPLICATION_JSON
            content = json
            with { it.remoteAddr = "198.51.100.1"; it }
        }.andExpect {
            status { isTooManyRequests() }
            jsonPath("$.status") { value(429) }
            jsonPath("$.error") { value("Too Many Requests") }
        }
    }
}
