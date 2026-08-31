package com.finance.application.use_case.user

import com.finance.application.dto.user.UpdateUserRequest
import com.finance.application.mapper.UserDtoMapper
import com.finance.application.service.InMemoryUserRepository
import com.finance.application.service.UserService
import com.finance.domain.entity.UserEntity
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.UserAlreadyExistsException
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UpdateUserUseCaseTest {

    private lateinit var userRepository: InMemoryUserRepository
    private lateinit var userService: UserService
    private lateinit var userDtoMapper: UserDtoMapper
    private lateinit var updateUserUseCase: UpdateUserUseCase

    @BeforeEach
    fun setup() {
        userRepository = InMemoryUserRepository()
        userService = UserService(userRepository)
        userDtoMapper = UserDtoMapper()
        updateUserUseCase = UpdateUserUseCase(
            userRepository = userRepository,
            userService = userService,
            userDtoMapper = userDtoMapper
        )
    }

    @Test
    fun `should update user details successfully`() {
        val user = UserEntity(
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur@finance.com"),
            passwordHash = PasswordHash("old_hash")
        )
        userRepository.save(user)

        val request = UpdateUserRequest(
            firstName = "Arthur Victor",
            lastName = "Menezes Silva",
            email = "arthur.silva@finance.com",
        )

        val response = updateUserUseCase.execute(
            userId = user.id,
            request = request,
            authenticatedUserId = user.id
        )

        assertEquals("Arthur Victor", response.firstName)
        assertEquals("Menezes Silva", response.lastName)
        assertEquals("arthur.silva@finance.com", response.email)

        val updatedEntity = userRepository.findById(user.id)
        assertNotNull(updatedEntity)
        assertEquals("old_hash", updatedEntity.passwordHash.value)
    }

    @Test
    fun `should fail update if authenticated user does not match target user`() {
        val user = UserEntity(
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur@finance.com"),
            passwordHash = PasswordHash("old_hash")
        )
        userRepository.save(user)

        val request = UpdateUserRequest(firstName = "New Name")

        assertThrows<AccessDeniedException> {
            updateUserUseCase.execute(
                userId = user.id,
                request = request,
                authenticatedUserId = UUID.randomUUID()
            )
        }
    }

    @Test
    fun `should fail update if new email is already taken by another user`() {
        val user1 = UserEntity(
            firstName = Name("User"),
            lastName = Name("One"),
            email = Email("user1@finance.com"),
            passwordHash = PasswordHash("hash1")
        )
        val user2 = UserEntity(
            firstName = Name("User"),
            lastName = Name("Two"),
            email = Email("user2@finance.com"),
            passwordHash = PasswordHash("hash2")
        )
        userRepository.save(user1)
        userRepository.save(user2)

        val request = UpdateUserRequest(email = "user2@finance.com")

        assertThrows<UserAlreadyExistsException> {
            updateUserUseCase.execute(userId = user1.id, request = request)
        }
    }
}
