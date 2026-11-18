package com.finance.application.dto.group

import com.finance.application.dto.category.CategoryParentSummary
import com.finance.domain.enum.CategoryLevel
import com.finance.domain.enum.CategoryType
import java.util.UUID

data class GroupUsedCategoryResponse(
    val id: UUID,
    val name: String,
    val color: String?,
    val icon: String?,
    val isPersonal: Boolean,
    val ownerId: UUID?,
    val type: CategoryType,
    val level: CategoryLevel,
    val parentId: UUID? = null,
    val parent: CategoryParentSummary? = null
)
