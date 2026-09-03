package com.finance.application.dto.group

import com.finance.domain.enum.GroupRole
import java.time.LocalDateTime
import java.util.UUID

data class GroupSummaryResponse(
    val id: UUID,
    val name: String,
    val myRole: GroupRole,
    val memberCount: Int,
    val createdAt: LocalDateTime,
)
