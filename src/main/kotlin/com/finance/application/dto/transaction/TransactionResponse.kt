package com.finance.application.dto.transaction

import com.finance.application.dto.group.GroupUserSummary
import com.finance.domain.enum.RecurrenceFrequency
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class TransactionResponse(
    val id: UUID,
    val title: String,
    val amount: Int,
    val date: LocalDate,
    val responsibleId: UUID,
    val responsible: GroupUserSummary? = null,
    val accountId: UUID,
    val type: TransactionType,
    val destinationAccountId: UUID?,
    val categoryId: UUID?,
    val status: TransactionStatus,
    val recurrenceGroupId: UUID?,
    val recurrenceFrequency: RecurrenceFrequency?,
    val recurrenceInterval: Int?,
    val isInfinite: Boolean,
    val creditCardId: UUID? = null,
    val invoiceMonth: Int? = null,
    val invoiceYear: Int? = null,
    val installmentNumber: Int? = null,
    val totalInstallments: Int? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
