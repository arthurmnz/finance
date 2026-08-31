package com.finance.domain.entity

import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import java.time.LocalDateTime
import java.util.UUID

class UserEntity(
    val id: UUID,
    firstName: Name,
    lastName: Name,
    email: Email,
    passwordHash: PasswordHash,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    var firstName: Name = firstName
        private set

    var lastName: Name = lastName
        private set

    var email: Email = email
        private set

    var passwordHash: PasswordHash = passwordHash
        private set

    var updatedAt: LocalDateTime = updatedAt
        private set

    constructor(
        firstName: Name,
        lastName: Name,
        email: Email,
        passwordHash: PasswordHash,
    ) : this(
        id = UUID.randomUUID(),
        firstName = firstName,
        lastName = lastName,
        email = email,
        passwordHash = passwordHash,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
    )

    fun updateName(newFirstName: Name, newLastName: Name) {
        this.firstName = newFirstName
        this.lastName = newLastName
        this.updatedAt = LocalDateTime.now()
    }

    fun updateEmail(newEmail: Email) {
        this.email = newEmail
        this.updatedAt = LocalDateTime.now()
    }

    fun changePassword(newPasswordHash: PasswordHash) {
        this.passwordHash = newPasswordHash
        this.updatedAt = LocalDateTime.now()
    }

    fun fullName(): String = "${firstName.formatted} ${lastName.formatted}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "UserEntity(id=$id, firstName=$firstName, lastName=$lastName, email=$email, createdAt=$createdAt, updatedAt=$updatedAt)"
    }
}