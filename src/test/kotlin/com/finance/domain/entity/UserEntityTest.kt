package com.finance.domain.entity

import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class UserEntityTest {

    @Test
    fun `should create user with generated ID`() {
        val user = UserEntity(
            firstName = Name("John"),
            lastName = Name("Doe"),
            email = Email("john.doe@example.com"),
            passwordHash = PasswordHash("hashed_pwd_123")
        )

        assertNotNull(user.id)
        assertEquals("John", user.firstName.value)
        assertEquals("Doe", user.lastName.value)
        assertEquals("john.doe@example.com", user.email.normalized)
        assertEquals("John Doe", user.fullName())
        assertNotNull(user.createdAt)
        assertNotNull(user.updatedAt)
    }

    @Test
    fun `should update user properties`() {
        val user = UserEntity(
            firstName = Name("John"),
            lastName = Name("Doe"),
            email = Email("john.doe@example.com"),
            passwordHash = PasswordHash("hashed_pwd_123")
        )

        val oldUpdatedAt = user.updatedAt

        user.updateName(Name("Jane"), Name("Smith"))
        assertEquals("Jane", user.firstName.value)
        assertEquals("Smith", user.lastName.value)
        assertEquals("Jane Smith", user.fullName())

        user.updateEmail(Email("jane.smith@example.com"))
        assertEquals("jane.smith@example.com", user.email.normalized)

        user.changePassword(PasswordHash("new_hashed_pwd_456"))
        assertEquals("new_hashed_pwd_456", user.passwordHash.value)
    }

    @Test
    fun `should check equality based on ID`() {
        val id = UUID.randomUUID()
        val user1 = UserEntity(
            id = id,
            firstName = Name("John"),
            lastName = Name("Doe"),
            email = Email("john.doe@example.com"),
            passwordHash = PasswordHash("pwd1")
        )

        val user2 = UserEntity(
            id = id,
            firstName = Name("Jane"),
            lastName = Name("Smith"),
            email = Email("jane.smith@example.com"),
            passwordHash = PasswordHash("pwd2")
        )

        val user3 = UserEntity(
            id = UUID.randomUUID(),
            firstName = Name("John"),
            lastName = Name("Doe"),
            email = Email("john.doe@example.com"),
            passwordHash = PasswordHash("pwd1")
        )

        assertEquals(user1, user2)
        assertEquals(user1.hashCode(), user2.hashCode())
        assertNotEquals(user1, user3)
    }
}
