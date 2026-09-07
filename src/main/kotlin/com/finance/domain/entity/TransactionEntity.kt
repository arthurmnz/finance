package com.finance.domain.entity

import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import java.time.LocalDateTime
import java.util.UUID

class TransactionEntity(
    val id: UUID,
    title: String,
    amount: Int,
    date: LocalDateTime,
    responsibleId: UUID,
    accountId: UUID,
    type: TransactionType,
    destinationAccountId: UUID?,
    categoryId: UUID?,
    status: TransactionStatus,
    val recurrenceGroupId: UUID? = null,
    val recurrenceFrequency: com.finance.domain.enum.RecurrenceFrequency? = null,
    val recurrenceInterval: Int? = null,
    val isInfinite: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now()
) {
    var title: String = title
        private set

    var amount: Int = amount
        private set

    var date: LocalDateTime = date
        private set

    var responsibleId: UUID = responsibleId
        private set

    var accountId: UUID = accountId
        private set

    var type: TransactionType = type
        private set

    var destinationAccountId: UUID? = destinationAccountId
        private set
        
    var categoryId: UUID? = categoryId
        private set
        
    var status: TransactionStatus = status
        private set

    var updatedAt: LocalDateTime = updatedAt
        private set

    init {
        if (type == TransactionType.TRANSFER) {
            requireNotNull(destinationAccountId) { "Destination account is required for transfers" }
        } else {
            require(destinationAccountId == null) { "Destination account must be null for non-transfers" }
        }
    }

    constructor(
        title: String,
        amount: Int,
        date: LocalDateTime,
        responsibleId: UUID,
        accountId: UUID,
        type: TransactionType,
        destinationAccountId: UUID?,
        categoryId: UUID?,
        status: TransactionStatus,
        recurrenceGroupId: UUID? = null,
        recurrenceFrequency: com.finance.domain.enum.RecurrenceFrequency? = null,
        recurrenceInterval: Int? = null,
        isInfinite: Boolean = false
    ) : this(
        id = UUID.randomUUID(),
        title = title,
        amount = amount,
        date = date,
        responsibleId = responsibleId,
        accountId = accountId,
        type = type,
        destinationAccountId = destinationAccountId,
        categoryId = categoryId,
        status = status,
        recurrenceGroupId = recurrenceGroupId,
        recurrenceFrequency = recurrenceFrequency,
        recurrenceInterval = recurrenceInterval,
        isInfinite = isInfinite,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    fun update(
        newTitle: String,
        newAmount: Int,
        newDate: LocalDateTime,
        newCategoryId: UUID?,
        newStatus: TransactionStatus
    ) {
        this.title = newTitle
        this.amount = newAmount
        this.date = newDate
        this.categoryId = newCategoryId
        this.status = newStatus
        this.updatedAt = LocalDateTime.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransactionEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
