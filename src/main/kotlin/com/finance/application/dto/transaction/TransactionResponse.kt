package com.finance.application.dto.transaction

import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import java.time.LocalDateTime
import java.util.UUID

data class TransactionResponse(
    val id: UUID,
    val title: String,
    val amount: Int,
    val date: LocalDateTime,
    val responsibleId: UUID,
    val accountId: UUID,
    val type: TransactionType,
    val destinationAccountId: UUID?,
    val categoryId: UUID?,
    val status: TransactionStatus,
    val recurrenceGroupId: UUID?,
    val recurrenceFrequency: com.finance.domain.enum.RecurrenceFrequency?,
    val recurrenceInterval: Int?,
    val isInfinite: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
