package com.finance.infraestructure.persistence.repository

import com.finance.domain.enum.CategoryLevel
import com.finance.infraestructure.persistence.entity.CategoryJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataCategoryRepository : JpaRepository<CategoryJpaEntity, UUID> {
    fun findByUserId(userId: UUID): List<CategoryJpaEntity>
    fun findByUserIdAndLevel(userId: UUID, level: CategoryLevel): List<CategoryJpaEntity>
    fun findByGroupId(groupId: UUID): List<CategoryJpaEntity>
    fun findByGroupIdAndLevel(groupId: UUID, level: CategoryLevel): List<CategoryJpaEntity>
    fun findByParentId(parentId: UUID): List<CategoryJpaEntity>
    fun existsByParentId(parentId: UUID): Boolean
    fun findByIdIn(ids: Collection<UUID>): List<CategoryJpaEntity>
    fun findByHierarchyMigratedFalse(): List<CategoryJpaEntity>
}
