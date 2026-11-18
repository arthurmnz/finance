package com.finance.application.use_case.group

import com.finance.application.dto.group.GroupUsedCategoryResponse
import com.finance.application.mapper.CategoryBatchLoader
import com.finance.application.mapper.CategoryDtoMapper
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
class GetGroupUsedCategoriesUseCase(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val accountRepository: AccountRepository,
    private val creditCardRepository: CreditCardRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    @Transactional(readOnly = true)
    fun execute(groupId: UUID, userId: UUID): List<GroupUsedCategoryResponse> {
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

        return allCategories.values.map { cat ->
            val parent = cat.parentId?.let { allCategories[it] }
            GroupUsedCategoryResponse(
                id = cat.id,
                name = cat.name,
                color = cat.color,
                icon = cat.icon,
                isPersonal = cat.userId != null,
                ownerId = cat.userId,
                type = cat.type,
                level = cat.level,
                parentId = cat.parentId,
                parent = parent?.let { CategoryDtoMapper.toParentSummary(it) }
            )
        }.sortedBy { it.name.lowercase() }
    }
}
