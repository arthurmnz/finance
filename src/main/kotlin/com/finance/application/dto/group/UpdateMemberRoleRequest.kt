package com.finance.application.dto.group

import com.finance.domain.enum.GroupRole

data class UpdateMemberRoleRequest(
    val email: String,
    val role: GroupRole,
)
