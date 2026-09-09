package com.finance.application.dto.transaction

import com.finance.domain.enum.RecurrenceFrequency
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import java.time.LocalDateTime
import java.util.UUID

data class CreateTransactionRequest(
    val title: String,
    val amount: Int,
    val date: LocalDateTime,
    val accountId: UUID,
    val type: TransactionType,
    val destinationAccountId: UUID? = null,
    val categoryId: UUID? = null,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val isRecurring: Boolean = false,
    val recurrenceFrequency: RecurrenceFrequency? = null,
    val recurrenceInterval: Int = 1,
    val recurrenceInstallments: Int = 12,
    val isInfinite: Boolean = false
)

data class UpdateTransactionRequest(
    val title: String,
    val amount: Int,
    val date: LocalDateTime,
    val categoryId: UUID? = null,
    val status: TransactionStatus,
    val updateAllFuture: Boolean = false,
    val isRecurring: Boolean? = null,
    val recurrenceFrequency: RecurrenceFrequency? = null,
    val recurrenceInterval: Int? = null,
    val recurrenceInstallments: Int? = null,
    val isInfinite: Boolean? = null
)
