package com.finance.application.dto.category

import com.finance.domain.enum.CategoryType
import java.time.LocalDateTime
import java.util.UUID

data class CategoryResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val userId: UUID?,
    val groupId: UUID?,
    val type: CategoryType,
    val color: String?,
    val icon: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
