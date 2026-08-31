package com.finance.infraestructure.persistence.repository

import com.finance.domain.entity.UserEntity
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
@Transactional
class UserRepositoryImplTest @Autowired constructor(
    private val userRepository: UserRepositoryImpl,
) {

    @Test
    fun `should save and find user by id`() {
        val user = UserEntity(
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur@finance.com"),
            passwordHash = PasswordHash("hashed123")
        )

        val savedUser = userRepository.save(user)
        assertNotNull(savedUser)
        assertEquals(user.id, savedUser.id)

        val foundUser = userRepository.findById(user.id)
        assertNotNull(foundUser)
        assertEquals("Arthur", foundUser.firstName.value)
        assertEquals("Menezes", foundUser.lastName.value)
        assertEquals("arthur@finance.com", foundUser.email.normalized)
    }

    @Test
    fun `should find user by email and verify existence`() {
        val user = UserEntity(
            firstName = Name("Carlos"),
            lastName = Name("Silva"),
            email = Email("carlos@finance.com"),
            passwordHash = PasswordHash("hashed456")
        )

        userRepository.save(user)

        val exists = userRepository.existsByEmail(Email("carlos@finance.com"))
        assertTrue(exists)

        val notExists = userRepository.existsByEmail(Email("nonexistent@finance.com"))
        assertFalse(notExists)

        val found = userRepository.findByEmail(Email("carlos@finance.com"))
        assertNotNull(found)
        assertEquals("Carlos", found.firstName.value)
    }

    @Test
    fun `should list all users`() {
        val user1 = UserEntity(
            firstName = Name("User"),
            lastName = Name("One"),
            email = Email("user1@finance.com"),
            passwordHash = PasswordHash("pwd1")
        )
        val user2 = UserEntity(
            firstName = Name("User"),
            lastName = Name("Two"),
            email = Email("user2@finance.com"),
            passwordHash = PasswordHash("pwd2")
        )

        userRepository.save(user1)
        userRepository.save(user2)

        val all = userRepository.findAll()
        assertTrue(all.size >= 2)
    }

    @Test
    fun `should delete user by id`() {
        val user = UserEntity(
            firstName = Name("To"),
            lastName = Name("Delete"),
            email = Email("delete@finance.com"),
            passwordHash = PasswordHash("pwd")
        )

        userRepository.save(user)
        val deleted = userRepository.deleteById(user.id)
        assertTrue(deleted)

        val found = userRepository.findById(user.id)
        assertNull(found)

        val deleteAgain = userRepository.deleteById(UUID.randomUUID())
        assertFalse(deleteAgain)
    }
}
