package com.finance.application.mapper

import com.finance.application.dto.transaction.TransactionResponse
import com.finance.domain.entity.TransactionEntity

object TransactionDtoMapper {
    fun toResponse(entity: TransactionEntity): TransactionResponse {
        return TransactionResponse(
            id = entity.id,
            title = entity.title,
            amount = entity.amount,
            date = entity.date,
            responsibleId = entity.responsibleId,
            accountId = entity.accountId,
            type = entity.type,
            destinationAccountId = entity.destinationAccountId,
            categoryId = entity.categoryId,
            status = entity.status,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}
