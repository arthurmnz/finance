package com.finance.domain.repository

import com.finance.domain.entity.CategoryEntity
import com.finance.domain.enum.CategoryLevel
import java.util.UUID

interface CategoryRepository {
    fun save(category: CategoryEntity): CategoryEntity
    fun saveAll(categories: List<CategoryEntity>): List<CategoryEntity>
    fun findById(id: UUID): CategoryEntity?
    fun findByIdIn(ids: Collection<UUID>): List<CategoryEntity>
    fun findByUserId(userId: UUID): List<CategoryEntity>
    fun findByUserIdAndLevel(userId: UUID, level: CategoryLevel): List<CategoryEntity>
    fun findByGroupId(groupId: UUID): List<CategoryEntity>
    fun findByGroupIdAndLevel(groupId: UUID, level: CategoryLevel): List<CategoryEntity>
    fun findByParentId(parentId: UUID): List<CategoryEntity>
    fun existsByParentId(parentId: UUID): Boolean
    fun findByHierarchyMigratedFalse(): List<CategoryEntity>
    fun delete(id: UUID)
}
