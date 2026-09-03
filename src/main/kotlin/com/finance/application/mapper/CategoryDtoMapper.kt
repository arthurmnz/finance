package com.finance.application.mapper

import com.finance.application.dto.category.CategoryResponse
import com.finance.domain.entity.CategoryEntity

object CategoryDtoMapper {
    fun toResponse(entity: CategoryEntity): CategoryResponse {
        return CategoryResponse(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            userId = entity.userId,
            groupId = entity.groupId,
            type = entity.type,
            color = entity.color,
            icon = entity.icon,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}
