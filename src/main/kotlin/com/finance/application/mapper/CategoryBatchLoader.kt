package com.finance.application.mapper

import com.finance.application.service.AccountAccessService
import com.finance.domain.entity.CategoryEntity
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.CreditCardRepository
import com.finance.domain.repository.TransactionRepository
import java.util.UUID

/**
 * Carrega categorias e suas respectivas categorias pai em lote (no máximo 2 consultas)
 * para evitar problemas de N+1 ao serializar respostas de transações e listagens.
 */
object CategoryBatchLoader {
    fun loadForTransactions(
        categoryRepository: CategoryRepository,
        transactions: Collection<TransactionEntity>
    ): Map<UUID, CategoryEntity> {
        if (transactions.isEmpty()) return emptyMap()

        val directIds = transactions
            .flatMap { listOfNotNull(it.categoryId, it.generalCategoryId) }
            .distinct()
        if (directIds.isEmpty()) return emptyMap()

        val loaded = categoryRepository.findByIdIn(directIds).associateBy { it.id }
        val missingParentIds = loaded.values
            .mapNotNull { it.parentId }
            .distinct()
            .filter { !loaded.containsKey(it) }

        if (missingParentIds.isEmpty()) return loaded

        val parents = categoryRepository.findByIdIn(missingParentIds).associateBy { it.id }
        return loaded + parents
    }

    fun loadWithParents(
        categoryRepository: CategoryRepository,
        categoryIds: Collection<UUID>
    ): Pair<Map<UUID, CategoryEntity>, Map<UUID, CategoryEntity>> {
        val distinctIds = categoryIds.distinct()
        if (distinctIds.isEmpty()) return emptyMap<UUID, CategoryEntity>() to emptyMap()

        val categories = categoryRepository.findByIdIn(distinctIds).associateBy { it.id }
        val missingParentIds = categories.values
            .mapNotNull { it.parentId }
            .distinct()
            .filter { !categories.containsKey(it) }

        val extraParents = if (missingParentIds.isEmpty()) {
            emptyMap()
        } else {
            categoryRepository.findByIdIn(missingParentIds).associateBy { it.id }
        }

        return categories to (categories + extraParents)
    }

    fun loadParentsForCategories(
        categoryRepository: CategoryRepository,
        categories: Collection<CategoryEntity>
    ): Map<UUID, CategoryEntity> {
        if (categories.isEmpty()) return emptyMap()
        val existingById = categories.associateBy { it.id }
        val missingParentIds = categories
            .mapNotNull { it.parentId }
            .distinct()
            .filter { !existingById.containsKey(it) }

        val extraParents = if (missingParentIds.isEmpty()) {
            emptyMap()
        } else {
            categoryRepository.findByIdIn(missingParentIds).associateBy { it.id }
        }

        return existingById + extraParents
    }

    fun collectGroupCategoriesWithParents(
        groupId: UUID,
        userId: UUID,
        categoryRepository: CategoryRepository,
        accountRepository: AccountRepository,
        creditCardRepository: CreditCardRepository,
        transactionRepository: TransactionRepository,
        accountAccessService: AccountAccessService
    ): Map<UUID, CategoryEntity> {
        val groupCategories = categoryRepository.findByGroupId(groupId)
        val accounts = accountRepository.findAllByGroupId(groupId)
            .filter { accountAccessService.hasAccess(it, userId) }
        val cards = creditCardRepository.findAllByGroupId(groupId)

        val usedCategoryIds = mutableSetOf<UUID>()
        accounts.forEach { acc ->
            transactionRepository.findByAccountId(acc.id).forEach { tx ->
                if (tx.creditCardId == null) {
                    tx.categoryId?.let { usedCategoryIds.add(it) }
                    tx.generalCategoryId?.let { usedCategoryIds.add(it) }
                }
            }
        }
        cards.forEach { card ->
            transactionRepository.findByCreditCardId(card.id).forEach { tx ->
                tx.categoryId?.let { usedCategoryIds.add(it) }
                tx.generalCategoryId?.let { usedCategoryIds.add(it) }
            }
        }

        val allCategories = mutableMapOf<UUID, CategoryEntity>()
        groupCategories.forEach { allCategories[it.id] = it }

        val missingUsedIds = usedCategoryIds.filter { !allCategories.containsKey(it) }
        if (missingUsedIds.isNotEmpty()) {
            categoryRepository.findByIdIn(missingUsedIds).forEach { allCategories[it.id] = it }
        }

        // Garante que categorias gerais pai das categorias pessoais usadas no grupo
        // também façam parte do resultado para preservar a hierarquia nos filtros/relatórios.
        val missingParentIds = allCategories.values
            .mapNotNull { it.parentId }
            .distinct()
            .filter { !allCategories.containsKey(it) }
        if (missingParentIds.isNotEmpty()) {
            categoryRepository.findByIdIn(missingParentIds).forEach { allCategories[it.id] = it }
        }

        return allCategories
    }
}
