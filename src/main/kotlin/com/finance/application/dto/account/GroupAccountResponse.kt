package com.finance.application.dto.account

import com.finance.application.dto.group.GroupUserSummary
import com.finance.domain.enum.AccountType
import java.time.LocalDateTime
import java.util.UUID

data class GroupAccountResponse(
    val id: UUID,
    val name: String,
    val startBalance: Int,
    val balance: Int,
    val type: AccountType,
    val tax: Int?,
    val groupId: UUID?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val owner: GroupUserSummary
)
