package com.finance.application.use_case.user

import com.finance.application.service.InMemoryUserRepository
import com.finance.application.service.UserService
import com.finance.domain.entity.UserEntity
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.UserNotFoundException
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertNull

class DeleteUserUseCaseTest {

    private lateinit var userRepository: InMemoryUserRepository
    private lateinit var userService: UserService
    private lateinit var deleteUserUseCase: DeleteUserUseCase

    @BeforeEach
    fun setup() {
        userRepository = InMemoryUserRepository()
        userService = UserService(userRepository)
        deleteUserUseCase = DeleteUserUseCase(
            userRepository = userRepository,
            userService = userService
        )
    }

    @Test
    fun `should delete user successfully`() {
        val user = UserEntity(
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur@finance.com"),
            passwordHash = PasswordHash("hash123")
        )
        userRepository.save(user)

        deleteUserUseCase.execute(user.id, authenticatedUserId = user.id)

        assertNull(userRepository.findById(user.id))
        assertFalse(userRepository.existsByEmail(Email("arthur@finance.com")))
    }

    @Test
    fun `should fail deletion if user does not exist`() {
        assertThrows<UserNotFoundException> {
            deleteUserUseCase.execute(UUID.randomUUID())
        }
    }

    @Test
    fun `should fail deletion if authenticated user is different`() {
        val user = UserEntity(
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur@finance.com"),
            passwordHash = PasswordHash("hash123")
        )
        userRepository.save(user)

        assertThrows<AccessDeniedException> {
            deleteUserUseCase.execute(user.id, authenticatedUserId = UUID.randomUUID())
        }
    }
}
