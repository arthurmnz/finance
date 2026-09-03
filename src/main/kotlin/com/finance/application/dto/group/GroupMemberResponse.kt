package com.finance.application.dto.group

import com.finance.domain.enum.GroupRole
import java.time.LocalDateTime

data class GroupMemberResponse(
    val user: GroupUserSummary,
    val role: GroupRole,
    val joinedAt: LocalDateTime,
)
