package com.finance.application.dto.group

import com.finance.domain.enum.GroupRole
import java.time.LocalDateTime
import java.util.UUID

data class GroupResponse(
    val id: UUID,
    val name: String,
    val myRole: GroupRole,
    val memberCount: Int,
    val members: List<GroupMemberResponse>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
