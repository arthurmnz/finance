package com.finance.application.service

import com.finance.domain.entity.UserEntity
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.UserAlreadyExistsException
import com.finance.domain.exception.UserNotFoundException
import com.finance.domain.repository.UserRepository
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UserServiceTest {

    private lateinit var fakeUserRepository: InMemoryUserRepository
    private lateinit var userService: UserService

    @BeforeEach
    fun setup() {
        fakeUserRepository = InMemoryUserRepository()
        userService = UserService(fakeUserRepository)
    }

    @Test
    fun `should find user by id successfully`() {
        val user = UserEntity(
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur@finance.com"),
            passwordHash = PasswordHash("hash123")
        )
        fakeUserRepository.save(user)

        val found = userService.findById(user.id)
        assertNotNull(found)
        assertEquals(user.id, found.id)
    }

    @Test
    fun `should throw UserNotFoundException when user id not found`() {
        assertThrows<UserNotFoundException> {
            userService.findById(UUID.randomUUID())
        }
    }

    @Test
    fun `should find user by email successfully`() {
        val user = UserEntity(
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur@finance.com"),
            passwordHash = PasswordHash("hash123")
        )
        fakeUserRepository.save(user)

        val found = userService.findByEmail(Email("arthur@finance.com"))
        assertNotNull(found)
        assertEquals("arthur@finance.com", found.email.normalized)
    }

    @Test
    fun `should throw UserAlreadyExistsException if email exists`() {
        val user = UserEntity(
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur@finance.com"),
            passwordHash = PasswordHash("hash123")
        )
        fakeUserRepository.save(user)

        assertThrows<UserAlreadyExistsException> {
            userService.checkEmailAvailability(Email("arthur@finance.com"))
        }
    }

    @Test
    fun `should validate user ownership and permission`() {
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()

        // Same user should pass
        userService.validateUserOwnership(userId, userId)
        assertTrue(userService.canModifyUser(userId, userId))

        // Different user should throw AccessDeniedException
        assertThrows<AccessDeniedException> {
            userService.validateUserOwnership(userId, otherUserId)
        }
        assertFalse(userService.canModifyUser(userId, otherUserId))
    }
}

class InMemoryUserRepository : UserRepository {
    private val storage = mutableMapOf<UUID, UserEntity>()

    override fun save(user: UserEntity): UserEntity {
        storage[user.id] = user
        return user
    }

    override fun findById(id: UUID): UserEntity? = storage[id]

    override fun findByEmail(email: Email): UserEntity? =
        storage.values.firstOrNull { it.email.normalized == email.normalized }

    override fun existsByEmail(email: Email): Boolean =
        storage.values.any { it.email.normalized == email.normalized }

    override fun findAll(): List<UserEntity> = storage.values.toList()

    override fun deleteById(id: UUID): Boolean = storage.remove(id) != null
}
