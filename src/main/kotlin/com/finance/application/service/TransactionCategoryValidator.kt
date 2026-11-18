package com.finance.application.service

import com.finance.domain.entity.CategoryEntity
import com.finance.domain.enum.CategoryLevel
import com.finance.domain.enum.CategoryType
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.CategoryNotFoundException
import com.finance.domain.exception.InvalidTransactionException
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.GroupMemberRepository
import java.util.UUID

object TransactionCategoryValidator {
    fun validateForTransaction(
        categoryRepository: CategoryRepository,
        groupMemberRepository: GroupMemberRepository,
        categoryId: UUID,
        userId: UUID,
        transactionType: TransactionType
    ): CategoryEntity {
        val category = categoryRepository.findById(categoryId)
            ?: throw CategoryNotFoundException("Categoria não encontrada")

        if (category.userId != null && category.userId != userId) {
            throw AccessDeniedException("Você não tem permissão para usar uma categoria pessoal de outro usuário.")
        }
        if (category.groupId != null && !groupMemberRepository.existsByGroupIdAndUserId(category.groupId!!, userId)) {
            throw AccessDeniedException("Você não tem permissão para usar categorias de um grupo do qual não é membro.")
        }
        if (category.level != CategoryLevel.SPECIFIC) {
            throw InvalidTransactionException(
                "Transações só podem ser vinculadas a categorias específicas (subcategorias)."
            )
        }
        if (!isTypeCompatible(category.type, transactionType)) {
            throw InvalidTransactionException(
                "O tipo da categoria (${category.type}) é incompatível com a transação ($transactionType)."
            )
        }
        return category
    }

    fun validateGroupScope(category: CategoryEntity, involvedGroupIds: List<UUID?>) {
        val catGroupId = category.groupId ?: return
        if (involvedGroupIds.isEmpty() || involvedGroupIds.any { it != catGroupId }) {
            throw InvalidTransactionException(
                "Categoria de grupo só pode ser utilizada quando todas as contas ou o cartão da transação pertencem ao mesmo grupo."
            )
        }
    }

    fun isTypeCompatible(categoryType: CategoryType, transactionType: TransactionType): Boolean {
        return when (transactionType) {
            TransactionType.EXPENSE -> categoryType == CategoryType.EXPENSE || categoryType == CategoryType.BOTH
            TransactionType.INCOME -> categoryType == CategoryType.INCOME || categoryType == CategoryType.BOTH
            TransactionType.TRANSFER -> true
        }
    }
}
