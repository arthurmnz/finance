package com.finance.application.dto.account

import com.finance.application.dto.group.GroupUserSummary
import com.finance.domain.enum.AccountGroupRequestStatus
import java.time.LocalDateTime
import java.util.UUID

data class AccountGroupRequestResponse(
    val id: UUID,
    val account: AccountSummary,
    val requester: GroupUserSummary,
    val status: AccountGroupRequestStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    data class AccountSummary(
        val id: UUID,
        val name: String
    )
}
