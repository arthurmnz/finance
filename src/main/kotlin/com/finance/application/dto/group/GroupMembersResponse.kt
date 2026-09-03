package com.finance.application.dto.group

import java.util.UUID

data class GroupMembersResponse(
    val groupId: UUID,
    val groupName: String,
    val members: List<GroupMemberResponse>,
)
