package com.finance.infraestructure.persistence.mapper

import com.finance.domain.entity.UserEntity
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import com.finance.infraestructure.persistence.entity.UserJpaEntity
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals

class UserMapperTest {

    private val userMapper = UserMapper()

    @Test
    fun `should map domain entity to JPA entity`() {
        val id = UUID.randomUUID()
        val createdAt = LocalDateTime.now().minusDays(1)
        val updatedAt = LocalDateTime.now()

        val domainUser = UserEntity(
            id = id,
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur@example.com"),
            passwordHash = PasswordHash("\$2a\$12\$hashValue"),
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

        val jpaUser = userMapper.toJpaEntity(domainUser)

        assertEquals(id, jpaUser.id)
        assertEquals("Arthur", jpaUser.firstName)
        assertEquals("Menezes", jpaUser.lastName)
        assertEquals("arthur@example.com", jpaUser.email)
        assertEquals("\$2a\$12\$hashValue", jpaUser.passwordHash)
        assertEquals(createdAt, jpaUser.createdAt)
        assertEquals(updatedAt, jpaUser.updatedAt)
    }

    @Test
    fun `should map JPA entity to domain entity`() {
        val id = UUID.randomUUID()
        val createdAt = LocalDateTime.now().minusDays(1)
        val updatedAt = LocalDateTime.now()

        val jpaUser = UserJpaEntity(
            id = id,
            firstName = "Arthur",
            lastName = "Menezes",
            email = "arthur@example.com",
            passwordHash = "\$2a\$12\$hashValue",
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

        val domainUser = userMapper.toDomain(jpaUser)

        assertEquals(id, domainUser.id)
        assertEquals("Arthur", domainUser.firstName.value)
        assertEquals("Menezes", domainUser.lastName.value)
        assertEquals("arthur@example.com", domainUser.email.normalized)
        assertEquals("\$2a\$12\$hashValue", domainUser.passwordHash.value)
        assertEquals(createdAt, domainUser.createdAt)
        assertEquals(updatedAt, domainUser.updatedAt)
    }
}
