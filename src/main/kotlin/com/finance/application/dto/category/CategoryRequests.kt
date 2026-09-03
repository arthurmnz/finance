package com.finance.application.dto.category

import com.finance.domain.enum.CategoryType
import java.util.UUID

data class CreateCategoryRequest(
    val name: String,
    val description: String?,
    val type: CategoryType,
    val color: String?,
    val icon: String?,
    val groupId: UUID? = null
)

data class UpdateCategoryRequest(
    val name: String,
    val description: String?,
    val type: CategoryType,
    val color: String?,
    val icon: String?
)
