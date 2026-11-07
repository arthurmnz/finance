package com.finance.application.dto.category

import com.finance.domain.enum.CategoryType
import java.util.UUID

data class CategorySummaryResponse(
    val id: UUID,
    val name: String,
    val color: String?,
    val icon: String?,
    val type: CategoryType
)
