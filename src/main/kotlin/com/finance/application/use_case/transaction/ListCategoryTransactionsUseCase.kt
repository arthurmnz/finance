package com.finance.application.use_case.transaction

import com.finance.application.dto.transaction.TransactionResponse
import com.finance.application.mapper.TransactionDtoMapper
import com.finance.application.service.AccountAccessService
import com.finance.domain.entity.CategoryEntity
import com.finance.domain.enum.CategoryLevel
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.CategoryNotFoundException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.CreditCardRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ListCategoryTransactionsUseCase(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val creditCardRepository: CreditCardRepository,
    private val accountAccessService: AccountAccessService
) {
    @Transactional(readOnly = true)
    fun execute(
        categoryId: UUID,
        month: Int? = null,
        year: Int? = null,
        authenticatedUserId: UUID
    ): List<TransactionResponse> {
        month?.let {
            if (month !in 1..12)
                throw IllegalArgumentException("O mês deve estar entre 1 e 12.")
        }

        val category = categoryRepository.findById(categoryId) ?: throw CategoryNotFoundException()
        val allowed = category.userId == authenticatedUserId ||
            (category.groupId != null && groupMemberRepository.existsByGroupIdAndUserId(category.groupId!!, authenticatedUserId))
        if (!allowed) {
            throw AccessDeniedException("Você não tem acesso às transações desta categoria.")
        }

        val childCategories = if (category.level == CategoryLevel.GENERAL) {
            categoryRepository.findByParentId(category.id)
                .filter { it.userId == category.userId && it.groupId == category.groupId }
        } else {
            emptyList()
        }

        val rawTransactions = if (category.level == CategoryLevel.GENERAL) {
            val ids = listOf(category.id) + childCategories.map { it.id }
            transactionRepository.findByCategoryIdInOrGeneralCategoryId(ids, category.id)
        } else {
            transactionRepository.findByCategoryId(categoryId)
        }

        val groupId = category.groupId
        val scopedTransactions = if (groupId != null) {
            val groupAccounts = accountRepository.findAllByGroupId(groupId)
            val accessibleGroupAccountIds = groupAccounts
                .filter { accountAccessService.hasAccess(it, authenticatedUserId) }
                .map { it.id }
                .toSet()
            val restrictedJointAccountIds = groupAccounts
                .filter { it.isJoint && !accountAccessService.hasAccess(it, authenticatedUserId) }
                .map { it.id }
                .toSet()
            val groupCardIds = creditCardRepository.findAllByGroupId(groupId).map { it.id }.toSet()
            rawTransactions.filter { tx ->
                if (tx.creditCardId == null && tx.accountId in restrictedJointAccountIds && tx.destinationAccountId !in accessibleGroupAccountIds) {
                    false
                } else {
                    (tx.creditCardId == null && (tx.accountId in accessibleGroupAccountIds || (tx.destinationAccountId != null && tx.destinationAccountId in accessibleGroupAccountIds))) ||
                        (tx.creditCardId != null && tx.creditCardId in groupCardIds) ||
                        (tx.responsibleId == authenticatedUserId && tx.accountId !in restrictedJointAccountIds)
                }
            }
        } else {
            val own = rawTransactions.filter { tx -> tx.responsibleId == authenticatedUserId }
            val accountIds = own
                .filter { it.creditCardId == null }
                .flatMap { listOfNotNull(it.accountId, it.destinationAccountId) }
                .toSet()
            val accountMap = if (accountIds.isEmpty()) emptyMap()
            else accountRepository.findAllByIdIn(accountIds).associateBy { it.id }
            own.filter { tx ->
                tx.creditCardId != null ||
                    listOfNotNull(tx.accountId, tx.destinationAccountId)
                        .mapNotNull { accountMap[it] }
                        .all { accountAccessService.hasAccess(it, authenticatedUserId) }
            }
        }

        val filteredTransactions = scopedTransactions
            .distinctBy { it.id }
            .filter {
                (month == null || it.date.monthValue == month) &&
                    (year == null || it.date.year == year)
            }

        if (filteredTransactions.isEmpty()) return emptyList()

        val knownCategories = mutableMapOf<UUID, CategoryEntity>()
        knownCategories[category.id] = category
        childCategories.forEach { knownCategories[it.id] = it }

        val missingCategoryIds = filteredTransactions
            .flatMap { listOfNotNull(it.categoryId, it.generalCategoryId) }
            .distinct()
            .filter { !knownCategories.containsKey(it) }
        if (missingCategoryIds.isNotEmpty()) {
            categoryRepository.findByIdIn(missingCategoryIds).forEach { knownCategories[it.id] = it }
        }

        val missingParentIds = knownCategories.values
            .mapNotNull { it.parentId }
            .distinct()
            .filter { !knownCategories.containsKey(it) }
        if (missingParentIds.isNotEmpty()) {
            categoryRepository.findByIdIn(missingParentIds).forEach { knownCategories[it.id] = it }
        }

        val users = userRepository.findAll().associateBy { it.id }
        return filteredTransactions.map { tx ->
            val cat = tx.categoryId?.let { knownCategories[it] }
            val parent = cat?.parentId?.let { knownCategories[it] }
            val generalCat = tx.generalCategoryId?.let { knownCategories[it] }
            TransactionDtoMapper.toResponse(tx, users[tx.responsibleId], cat, parent, generalCat)
        }
    }
}
