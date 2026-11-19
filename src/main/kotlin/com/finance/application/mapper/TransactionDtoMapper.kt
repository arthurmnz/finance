package com.finance.application.mapper

import com.finance.application.dto.category.CategorySummaryResponse
import com.finance.application.dto.group.GroupUserSummary
import com.finance.application.dto.transaction.TransactionResponse
import com.finance.domain.entity.CategoryEntity
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.entity.UserEntity
import com.finance.domain.enum.CategoryLevel

object TransactionDtoMapper {
    fun toCategorySummary(
        category: CategoryEntity,
        parentCategory: CategoryEntity? = null
    ): CategorySummaryResponse {
        val parent = if (category.level == CategoryLevel.SPECIFIC) parentCategory else null
        return CategorySummaryResponse(
            id = category.id,
            name = category.name,
            color = category.color,
            icon = category.icon,
            type = category.type,
            level = category.level,
            parentId = category.parentId,
            parent = parent?.let { CategoryDtoMapper.toParentSummary(it) }
        )
    }

    fun toResponse(
        entity: TransactionEntity,
        responsibleUser: UserEntity? = null,
        category: CategoryEntity? = null,
        parentCategory: CategoryEntity? = null,
        generalCategoryEntity: CategoryEntity? = null
    ): TransactionResponse {
        val summary = responsibleUser?.let {
            GroupUserSummary(
                id = it.id,
                firstName = it.firstName.value,
                lastName = it.lastName.value,
                email = it.email.normalized
            )
        }

        val resolvedSpecificEntity: CategoryEntity? = when {
            category?.level == CategoryLevel.SPECIFIC -> category
            else -> null
        }

        val resolvedGeneralEntity: CategoryEntity? = when {
            generalCategoryEntity != null && generalCategoryEntity.level == CategoryLevel.GENERAL -> generalCategoryEntity
            category?.level == CategoryLevel.GENERAL -> category
            parentCategory?.level == CategoryLevel.GENERAL -> parentCategory
            else -> generalCategoryEntity ?: parentCategory
        }

        val effectiveParentForSpecific = when {
            resolvedSpecificEntity == null -> null
            parentCategory != null -> parentCategory
            resolvedGeneralEntity?.id == resolvedSpecificEntity.parentId -> resolvedGeneralEntity
            else -> null
        }

        val specificSummary = resolvedSpecificEntity?.let { toCategorySummary(it, effectiveParentForSpecific) }
        val generalSummary = resolvedGeneralEntity?.let { toCategorySummary(it, null) }

        val effectiveCategorySummary = when {
            specificSummary != null -> specificSummary
            category != null -> toCategorySummary(category, parentCategory)
            resolvedGeneralEntity != null && entity.categoryId == resolvedGeneralEntity.id -> generalSummary
            else -> null
        }

        val resolvedGeneralId = entity.generalCategoryId
            ?: resolvedGeneralEntity?.id
            ?: resolvedSpecificEntity?.parentId
            ?: category?.takeIf { it.level == CategoryLevel.GENERAL }?.id

        val resolvedSpecificId = when {
            resolvedSpecificEntity != null -> resolvedSpecificEntity.id
            entity.categoryId != null && entity.generalCategoryId != null && entity.categoryId != entity.generalCategoryId -> entity.categoryId
            else -> null
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
            category = effectiveCategorySummary,
            generalCategoryId = resolvedGeneralId,
            specificCategoryId = resolvedSpecificId,
            generalCategory = generalSummary,
            specificCategory = specificSummary,
            status = entity.status,
            recurrenceGroupId = entity.recurrenceGroupId,
            recurrenceFrequency = entity.recurrenceFrequency,
            recurrenceInterval = entity.recurrenceInterval,
            isInfinite = entity.isInfinite,
            creditCardId = entity.creditCardId,
            creditCardInvoiceId = entity.creditCardInvoiceId,
            invoiceMonth = entity.invoiceMonth,
            invoiceYear = entity.invoiceYear,
            installmentNumber = entity.installmentNumber,
            totalInstallments = entity.totalInstallments,
            generatesDebt = entity.generatesDebt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}
