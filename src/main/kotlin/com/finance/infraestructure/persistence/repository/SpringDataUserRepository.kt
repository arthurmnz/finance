package com.finance.infraestructure.persistence.repository

import com.finance.infraestructure.persistence.entity.UserJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SpringDataUserRepository : JpaRepository<UserJpaEntity, UUID> {
    fun findByEmail(email: String): UserJpaEntity?
    fun existsByEmail(email: String): Boolean
}
