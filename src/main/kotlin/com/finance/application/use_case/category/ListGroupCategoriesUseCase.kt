package com.finance.application.use_case.category

import com.finance.application.dto.category.CategoryResponse
import com.finance.application.mapper.CategoryBatchLoader
import com.finance.application.mapper.CategoryDtoMapper
import com.finance.domain.enum.CategoryLevel
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
    fun execute(
        groupId: UUID,
        userId: UUID,
        level: CategoryLevel? = null,
        parentId: UUID? = null
    ): List<CategoryResponse> {
        groupRepository.findById(groupId) ?: throw GroupNotFoundException()
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw AccessDeniedException("Você não tem acesso a este grupo.")
        }

        val allCategories = CategoryBatchLoader.collectGroupCategoriesWithParents(
            groupId = groupId,
            categoryRepository = categoryRepository,
            accountRepository = accountRepository,
            creditCardRepository = creditCardRepository,
            transactionRepository = transactionRepository
        )

        return allCategories.values
            .asSequence()
            .filter { level == null || it.level == level }
            .filter { parentId == null || it.parentId == parentId }
            .map { cat -> CategoryDtoMapper.toResponse(cat, cat.parentId?.let { allCategories[it] }) }
            .sortedBy { it.name.lowercase() }
            .toList()
    }
}
