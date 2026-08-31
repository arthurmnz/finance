package com.finance.domain.repository

import com.finance.domain.entity.UserEntity
import com.finance.domain.value_object.Email
import java.util.UUID

interface UserRepository {
    fun save(user: UserEntity): UserEntity
    fun findById(id: UUID): UserEntity?
    fun findByEmail(email: Email): UserEntity?
    fun existsByEmail(email: Email): Boolean
    fun findAll(): List<UserEntity>
    fun deleteById(id: UUID): Boolean
}
