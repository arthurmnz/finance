package com.finance.infraestructure.persistence.repository

import com.finance.infraestructure.persistence.entity.CategoryJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataCategoryRepository : JpaRepository<CategoryJpaEntity, UUID> {
    fun findByUserId(userId: UUID): List<CategoryJpaEntity>
    fun findByGroupId(groupId: UUID): List<CategoryJpaEntity>
}
