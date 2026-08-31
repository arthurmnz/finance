package com.finance.presentation.controller

import com.finance.application.dto.auth.RegisterUserRequest
import com.finance.application.mapper.UserDtoMapper
import com.finance.application.service.InMemoryUserRepository
import com.finance.application.service.UserService
import com.finance.application.use_case.auth.FakePasswordEncoder
import com.finance.application.use_case.auth.RegisterUserUseCase
import com.finance.presentation.exception.GlobalExceptionHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class AuthControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var userRepository: InMemoryUserRepository
    private lateinit var userService: UserService
    private lateinit var registerUserUseCase: RegisterUserUseCase

    @BeforeEach
    fun setup() {
        userRepository = InMemoryUserRepository()
        userService = UserService(userRepository)
        val passwordEncoder = FakePasswordEncoder()
        val userDtoMapper = UserDtoMapper()
        registerUserUseCase = RegisterUserUseCase(
            userRepository = userRepository,
            userService = userService,
            passwordEncoder = passwordEncoder,
            userDtoMapper = userDtoMapper
        )
        val authController = AuthController(registerUserUseCase)

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
                "password": "SecretPassword123"
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
    fun `should return 409 Conflict when email already registered`() {
        val json = """
            {
                "firstName": "Arthur",
                "lastName": "Menezes",
                "email": "duplicate@finance.com",
                "password": "SecretPassword123"
            }
        """.trimIndent()

        mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isCreated() }
        }

        mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isConflict() }
            jsonPath("$.status") { value(409) }
            jsonPath("$.error") { value("Conflict") }
        }
    }

    @Test
    fun `should return 400 Bad Request when validation fails in Value Objects`() {
        val json = """
            {
                "firstName": "   ",
                "lastName": "Menezes",
                "email": "invalid-email",
                "password": "SecretPassword123"
            }
        """.trimIndent()

        mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = json
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.status") { value(400) }
            jsonPath("$.error") { value("Bad Request") }
        }
    }
}
