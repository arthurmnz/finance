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

data class ResolvedTransactionCategories(
    val generalCategory: CategoryEntity? = null,
    val specificCategory: CategoryEntity? = null
) {
    val effectiveGeneralCategoryId: UUID?
        get() = generalCategory?.id

    val effectiveCategoryId: UUID?
        get() = specificCategory?.id ?: generalCategory?.id

    val allCategories: List<CategoryEntity>
        get() = listOfNotNull(generalCategory, specificCategory)
}

object TransactionCategoryValidator {
    fun validateSelection(
        categoryRepository: CategoryRepository,
        groupMemberRepository: GroupMemberRepository,
        userId: UUID,
        transactionType: TransactionType,
        categoryId: UUID? = null,
        generalCategoryId: UUID? = null,
        specificCategoryId: UUID? = null
    ): ResolvedTransactionCategories {
        if (specificCategoryId != null && categoryId != null && specificCategoryId != categoryId) {
            throw InvalidTransactionException(
                "Os campos specificCategoryId e categoryId estão em conflito."
            )
        }

        if (generalCategoryId == null && specificCategoryId == null && categoryId == null) {
            return ResolvedTransactionCategories()
        }

        if (generalCategoryId != null) {
            val general = loadAndValidateAccess(
                categoryRepository = categoryRepository,
                groupMemberRepository = groupMemberRepository,
                categoryId = generalCategoryId,
                userId = userId,
                notFoundMessage = "Categoria geral não encontrada"
            )
            if (general.level != CategoryLevel.GENERAL) {
                throw InvalidTransactionException(
                    "O campo generalCategoryId deve referenciar uma categoria geral."
                )
            }
            validateTypeCompatibility(general, transactionType)

            val candidateSpecificId = specificCategoryId
                ?: categoryId?.takeIf { it != generalCategoryId }
                ?: return ResolvedTransactionCategories(generalCategory = general, specificCategory = null)

            val specific = loadAndValidateAccess(
                categoryRepository = categoryRepository,
                groupMemberRepository = groupMemberRepository,
                categoryId = candidateSpecificId,
                userId = userId,
                notFoundMessage = "Categoria específica não encontrada"
            )
            if (specific.level != CategoryLevel.SPECIFIC) {
                throw InvalidTransactionException(
                    "A categoria específica selecionada deve pertencer ao nível SPECIFIC."
                )
            }
            if (specific.parentId != general.id) {
                throw InvalidTransactionException(
                    "A categoria específica selecionada não pertence à categoria geral informada."
                )
            }
            validateTypeCompatibility(specific, transactionType)
            return ResolvedTransactionCategories(generalCategory = general, specificCategory = specific)
        }

        if (specificCategoryId != null) {
            val specific = loadAndValidateAccess(
                categoryRepository = categoryRepository,
                groupMemberRepository = groupMemberRepository,
                categoryId = specificCategoryId,
                userId = userId,
                notFoundMessage = "Categoria específica não encontrada"
            )
            if (specific.level != CategoryLevel.SPECIFIC) {
                throw InvalidTransactionException(
                    "O campo specificCategoryId deve referenciar uma categoria específica."
                )
            }
            validateTypeCompatibility(specific, transactionType)

            val general = resolveAndValidateParentGeneral(
                categoryRepository = categoryRepository,
                groupMemberRepository = groupMemberRepository,
                specific = specific,
                userId = userId,
                transactionType = transactionType
            )
            return ResolvedTransactionCategories(generalCategory = general, specificCategory = specific)
        }

        val category = loadAndValidateAccess(
            categoryRepository = categoryRepository,
            groupMemberRepository = groupMemberRepository,
            categoryId = categoryId!!,
            userId = userId,
            notFoundMessage = "Categoria não encontrada"
        )
        validateTypeCompatibility(category, transactionType)

        if (category.level == CategoryLevel.GENERAL) {
            return ResolvedTransactionCategories(generalCategory = category, specificCategory = null)
        }

        val general = resolveAndValidateParentGeneral(
            categoryRepository = categoryRepository,
            groupMemberRepository = groupMemberRepository,
            specific = category,
            userId = userId,
            transactionType = transactionType
        )
        return ResolvedTransactionCategories(generalCategory = general, specificCategory = category)
    }

    private fun resolveAndValidateParentGeneral(
        categoryRepository: CategoryRepository,
        groupMemberRepository: GroupMemberRepository,
        specific: CategoryEntity,
        userId: UUID,
        transactionType: TransactionType
    ): CategoryEntity {
        val parentId = specific.parentId
            ?: throw InvalidTransactionException("Categoria específica sem categoria geral associada.")
        val general = loadAndValidateAccess(
            categoryRepository = categoryRepository,
            groupMemberRepository = groupMemberRepository,
            categoryId = parentId,
            userId = userId,
            notFoundMessage = "Categoria geral pai não encontrada"
        )
        if (general.level != CategoryLevel.GENERAL) {
            throw InvalidTransactionException("A categoria pai deve ser uma categoria geral.")
        }
        validateTypeCompatibility(general, transactionType)
        return general
    }

    private fun loadAndValidateAccess(
        categoryRepository: CategoryRepository,
        groupMemberRepository: GroupMemberRepository,
        categoryId: UUID,
        userId: UUID,
        notFoundMessage: String
    ): CategoryEntity {
        val category = categoryRepository.findById(categoryId)
            ?: throw CategoryNotFoundException(notFoundMessage)

        if (category.userId != null && category.userId != userId) {
            throw AccessDeniedException("Você não tem permissão para usar uma categoria pessoal de outro usuário.")
        }
        if (category.groupId != null && !groupMemberRepository.existsByGroupIdAndUserId(category.groupId!!, userId)) {
            throw AccessDeniedException("Você não tem permissão para usar categorias de um grupo do qual não é membro.")
        }
        return category
    }

    private fun validateTypeCompatibility(category: CategoryEntity, transactionType: TransactionType) {
        if (!isTypeCompatible(category.type, transactionType)) {
            throw InvalidTransactionException(
                "O tipo da categoria (${category.type}) é incompatível com a transação ($transactionType)."
            )
        }
    }

    fun validateForTransaction(
        categoryRepository: CategoryRepository,
        groupMemberRepository: GroupMemberRepository,
        categoryId: UUID,
        userId: UUID,
        transactionType: TransactionType
    ): CategoryEntity {
        val category = loadAndValidateAccess(
            categoryRepository = categoryRepository,
            groupMemberRepository = groupMemberRepository,
            categoryId = categoryId,
            userId = userId,
            notFoundMessage = "Categoria não encontrada"
        )
        validateTypeCompatibility(category, transactionType)
        return category
    }

    fun validateGroupScope(resolved: ResolvedTransactionCategories, involvedGroupIds: List<UUID?>) {
        resolved.allCategories.forEach { category ->
            validateGroupScope(category, involvedGroupIds)
        }
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
