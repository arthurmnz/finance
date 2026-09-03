package com.finance.domain.repository

import com.finance.domain.entity.CategoryEntity
import java.util.UUID

interface CategoryRepository {
    fun save(category: CategoryEntity): CategoryEntity
    fun findById(id: UUID): CategoryEntity?
    fun findByUserId(userId: UUID): List<CategoryEntity>
    fun findByGroupId(groupId: UUID): List<CategoryEntity>
    fun delete(id: UUID)
}
