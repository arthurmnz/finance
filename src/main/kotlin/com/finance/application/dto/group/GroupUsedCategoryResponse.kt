package com.finance.application.dto.group

import java.util.UUID

data class GroupUsedCategoryResponse(
    val id: UUID,
    val name: String,
    val color: String?,
    val icon: String?,
    val isPersonal: Boolean,
    val ownerId: UUID?
)
