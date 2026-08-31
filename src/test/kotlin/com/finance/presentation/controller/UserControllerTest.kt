package com.finance.presentation.controller

import com.finance.application.mapper.UserDtoMapper
import com.finance.application.service.InMemoryUserRepository
import com.finance.application.service.UserService
import com.finance.application.use_case.user.DeleteUserUseCase
import com.finance.application.use_case.user.UpdateUserUseCase
import com.finance.domain.entity.UserEntity
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import com.finance.presentation.exception.GlobalExceptionHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.util.UUID

class UserControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var userRepository: InMemoryUserRepository
    private lateinit var userService: UserService
    private lateinit var existingUser: UserEntity

    @BeforeEach
    fun setup() {
        userRepository = InMemoryUserRepository()
        userService = UserService(userRepository)
        val userDtoMapper = UserDtoMapper()
        val updateUserUseCase = UpdateUserUseCase(userRepository, userService, userDtoMapper)
        val deleteUserUseCase = DeleteUserUseCase(userRepository, userService)

        val userController = UserController(
            updateUserUseCase = updateUserUseCase,
            deleteUserUseCase = deleteUserUseCase,
            userService = userService,
            userDtoMapper = userDtoMapper
        )

        mockMvc = MockMvcBuilders
            .standaloneSetup(userController)
            .setControllerAdvice(GlobalExceptionHandler())
            .build()

        existingUser = UserEntity(
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur.controller@finance.com"),
            passwordHash = PasswordHash("hashed123")
        )
        userRepository.save(existingUser)
    }

    @Test
    fun `should get user by id with 200 OK`() {
        mockMvc.get("/api/v1/users/${existingUser.id}")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(existingUser.id.toString()) }
                jsonPath("$.firstName") { value("Arthur") }
                jsonPath("$.lastName") { value("Menezes") }
                jsonPath("$.email") { value("arthur.controller@finance.com") }
            }
    }

    @Test
    fun `should return 404 Not Found when user does not exist`() {
        val nonExistentId = UUID.randomUUID()
        mockMvc.get("/api/v1/users/$nonExistentId")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.status") { value(404) }
            }
    }

    @Test
    fun `should list all users with 200 OK`() {
        mockMvc.get("/api/v1/users")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].id") { exists() }
            }
    }

    @Test
    fun `should update user profile with 200 OK`() {
        val json = """
            {
                "firstName": "Arthur Victor",
                "lastName": "Menezes Silva",
                "email": "arthur.updated@finance.com"
            }
        """.trimIndent()

        mockMvc.put("/api/v1/users/${existingUser.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = json
            header("X-User-Id", existingUser.id.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.firstName") { value("Arthur Victor") }
            jsonPath("$.lastName") { value("Menezes Silva") }
            jsonPath("$.email") { value("arthur.updated@finance.com") }
        }
    }

    @Test
    fun `should return 403 Forbidden when updating another user`() {
        val otherUserId = UUID.randomUUID()
        val json = """
            {
                "firstName": "Hacker"
            }
        """.trimIndent()

        mockMvc.put("/api/v1/users/${existingUser.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = json
            header("X-User-Id", otherUserId.toString())
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.status") { value(403) }
        }
    }

    @Test
    fun `should delete user with 204 No Content`() {
        mockMvc.delete("/api/v1/users/${existingUser.id}") {
            header("X-User-Id", existingUser.id.toString())
        }.andExpect {
            status { isNoContent() }
        }

        mockMvc.get("/api/v1/users/${existingUser.id}")
            .andExpect {
                status { isNotFound() }
            }
    }
}
