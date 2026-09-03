package com.finance.application.use_case.auth

import com.finance.application.dto.auth.RegisterUserRequest
import com.finance.application.mapper.UserDtoMapper
import com.finance.application.port.PasswordEncoderPort
import com.finance.application.service.InMemoryUserRepository
import com.finance.application.service.UserService
import com.finance.domain.exception.UserAlreadyExistsException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RegisterUserUseCaseTest {

    private lateinit var userRepository: InMemoryUserRepository
    private lateinit var userService: UserService
    private lateinit var passwordEncoder: FakePasswordEncoder
    private lateinit var userDtoMapper: UserDtoMapper
    private lateinit var registerUserUseCase: RegisterUserUseCase

    @BeforeEach
    fun setup() {
        userRepository = InMemoryUserRepository()
        userService = UserService(userRepository)
        passwordEncoder = FakePasswordEncoder()
        userDtoMapper = UserDtoMapper()
        registerUserUseCase = RegisterUserUseCase(
            userRepository = userRepository,
            userService = userService,
            passwordEncoder = passwordEncoder,
            userDtoMapper = userDtoMapper
        )
    }

    @Test
    fun `should register user successfully with strong password`() {
        val request = RegisterUserRequest(
            firstName = "Arthur",
            lastName = "Menezes",
            email = "arthur@finance.com",
            password = "SecretPassword123!"
        )

        val response = registerUserUseCase.execute(request)

        assertNotNull(response.id)
        assertEquals("Arthur", response.firstName)
        assertEquals("Menezes", response.lastName)
        assertEquals("arthur@finance.com", response.email)

        val savedUser = userRepository.findById(response.id)
        assertNotNull(savedUser)
        assertTrue(savedUser.passwordHash.value.startsWith("hashed_"))
    }

    @Test
    fun `should fail when email is already registered`() {
        val request = RegisterUserRequest(
            firstName = "Arthur",
            lastName = "Menezes",
            email = "arthur@finance.com",
            password = "SecretPassword123!"
        )

        registerUserUseCase.execute(request)

        assertThrows<UserAlreadyExistsException> {
            registerUserUseCase.execute(request)
        }
    }

    @Test
    fun `should fail when password does not meet strength requirements`() {
        // Less than 8 chars
        assertThrows<IllegalArgumentException> {
            registerUserUseCase.execute(
                RegisterUserRequest("Arthur", "Menezes", "test1@finance.com", "Ab1!")
            )
        }

        // Missing uppercase
        assertThrows<IllegalArgumentException> {
            registerUserUseCase.execute(
                RegisterUserRequest("Arthur", "Menezes", "test2@finance.com", "secret123!")
            )
        }

        // Missing lowercase
        assertThrows<IllegalArgumentException> {
            registerUserUseCase.execute(
                RegisterUserRequest("Arthur", "Menezes", "test3@finance.com", "SECRET123!")
            )
        }

        // Missing digit
        assertThrows<IllegalArgumentException> {
            registerUserUseCase.execute(
                RegisterUserRequest("Arthur", "Menezes", "test4@finance.com", "SecretPassword!")
            )
        }

        // Missing special character
        assertThrows<IllegalArgumentException> {
            registerUserUseCase.execute(
                RegisterUserRequest("Arthur", "Menezes", "test5@finance.com", "SecretPassword123")
            )
        }
    }
}

class FakePasswordEncoder : PasswordEncoderPort {
    override fun encode(rawPassword: String): String = "hashed_$rawPassword"
    override fun matches(rawPassword: String, encodedPassword: String): Boolean =
        encodedPassword == "hashed_$rawPassword"
}
