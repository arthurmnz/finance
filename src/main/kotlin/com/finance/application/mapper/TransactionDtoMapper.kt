package com.finance.application.mapper

import com.finance.application.dto.group.GroupUserSummary
import com.finance.application.dto.transaction.TransactionResponse
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.entity.UserEntity

object TransactionDtoMapper {
    fun toResponse(entity: TransactionEntity, responsibleUser: UserEntity? = null): TransactionResponse {
        val summary = responsibleUser?.let {
            GroupUserSummary(
                id = it.id,
                firstName = it.firstName.value,
                lastName = it.lastName.value,
                email = it.email.normalized
            )
        }
        return TransactionResponse(
            id = entity.id,
            title = entity.title,
            amount = entity.amount,
            date = entity.date,
            responsibleId = entity.responsibleId,
            responsible = summary,
            accountId = entity.accountId,
            type = entity.type,
            destinationAccountId = entity.destinationAccountId,
            categoryId = entity.categoryId,
            status = entity.status,
            recurrenceGroupId = entity.recurrenceGroupId,
            recurrenceFrequency = entity.recurrenceFrequency,
            recurrenceInterval = entity.recurrenceInterval,
            isInfinite = entity.isInfinite,
            creditCardId = entity.creditCardId,
            invoiceMonth = entity.invoiceMonth,
            invoiceYear = entity.invoiceYear,
            installmentNumber = entity.installmentNumber,
            totalInstallments = entity.totalInstallments,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}
