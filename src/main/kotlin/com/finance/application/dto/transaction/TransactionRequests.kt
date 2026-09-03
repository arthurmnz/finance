package com.finance.application.dto.transaction

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
    val status: TransactionStatus = TransactionStatus.PENDING
)

data class UpdateTransactionRequest(
    val title: String,
    val amount: Int,
    val date: LocalDateTime,
    val categoryId: UUID? = null,
    val status: TransactionStatus
)
