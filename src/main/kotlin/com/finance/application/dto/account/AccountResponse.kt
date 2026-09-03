package com.finance.application.dto.account

import com.finance.domain.enum.AccountType
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class AccountResponse(
    val id: UUID,
    val ownerId: UUID,
    val name: String,
    val startBalance: Int,
    val balance: Int,
    val type: AccountType,
    val tax: Int?,
    val groupId: UUID?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
