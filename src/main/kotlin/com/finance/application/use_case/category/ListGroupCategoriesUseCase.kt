package com.finance.application.use_case.category

import com.finance.application.dto.category.CategoryResponse
import com.finance.application.mapper.CategoryDtoMapper
import com.finance.domain.entity.CategoryEntity
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.GroupNotFoundException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.CreditCardRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
import com.finance.domain.repository.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ListGroupCategoriesUseCase(
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val creditCardRepository: CreditCardRepository,
    private val transactionRepository: TransactionRepository,
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository
) {
    @Transactional(readOnly = true)
    fun execute(groupId: UUID, userId: UUID): List<CategoryResponse> {
        groupRepository.findById(groupId) ?: throw GroupNotFoundException()
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw AccessDeniedException("Você não tem acesso a este grupo.")
        }

        val groupCategories = categoryRepository.findByGroupId(groupId)

        val accounts = accountRepository.findAllByGroupId(groupId)
        val cards = creditCardRepository.findAllByGroupId(groupId)

        val categoryIds = mutableSetOf<UUID>()

        accounts.forEach { acc ->
            val txs = transactionRepository.findByAccountId(acc.id)
            txs.forEach { tx ->
                tx.categoryId?.let { categoryIds.add(it) }
            }
        }

        cards.forEach { card ->
            val txs = transactionRepository.findByCreditCardId(card.id)
            txs.forEach { tx ->
                tx.categoryId?.let { categoryIds.add(it) }
            }
        }

        val allCategories = mutableMapOf<UUID, CategoryEntity>()
        groupCategories.forEach { allCategories[it.id] = it }

        categoryIds.forEach { catId ->
            if (!allCategories.containsKey(catId)) {
                categoryRepository.findById(catId)?.let { allCategories[it.id] = it }
            }
        }

        return allCategories.values.map { CategoryDtoMapper.toResponse(it) }.sortedBy { it.name.lowercase() }
    }
}
