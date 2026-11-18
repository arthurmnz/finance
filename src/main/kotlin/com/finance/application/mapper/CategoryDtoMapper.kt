package com.finance.application.mapper

import com.finance.application.dto.category.CategoryParentSummary
import com.finance.application.dto.category.CategoryResponse
import com.finance.domain.entity.CategoryEntity

object CategoryDtoMapper {
    fun toParentSummary(entity: CategoryEntity): CategoryParentSummary {
        return CategoryParentSummary(
            id = entity.id,
            name = entity.name,
            color = entity.color,
            icon = entity.icon,
            type = entity.type
        )
    }

    fun toResponse(entity: CategoryEntity, parent: CategoryEntity? = null): CategoryResponse {
        return CategoryResponse(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            userId = entity.userId,
            groupId = entity.groupId,
            type = entity.type,
            color = entity.color,
            icon = entity.icon,
            level = entity.level,
            parentId = entity.parentId,
            parent = parent?.let { toParentSummary(it) },
            isDefault = entity.isDefault,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}
