package com.finance.application.dto.group

import java.util.UUID

data class GroupUserSummary(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val email: String,
)
