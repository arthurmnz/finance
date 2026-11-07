package com.finance.application.use_case.group

import com.finance.application.dto.group.GroupUsedCategoryResponse
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
        val group = groupRepository.findById(groupId) ?: throw GroupNotFoundException()
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw AccessDeniedException("Você não tem acesso a este grupo.")
        }

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

        val groupExplicitCategories = categoryRepository.findByGroupId(groupId)
        groupExplicitCategories.forEach { categoryIds.add(it.id) }

        val resultCategories = mutableMapOf<UUID, GroupUsedCategoryResponse>()

        groupExplicitCategories.forEach { cat ->
            resultCategories[cat.id] = GroupUsedCategoryResponse(
                id = cat.id,
                name = cat.name,
                color = cat.color,
                icon = cat.icon,
                isPersonal = cat.userId != null,
                ownerId = cat.userId
            )
        }

        categoryIds.forEach { catId ->
            if (!resultCategories.containsKey(catId)) {
                val cat = categoryRepository.findById(catId)
                if (cat != null) {
                    resultCategories[cat.id] = GroupUsedCategoryResponse(
                        id = cat.id,
                        name = cat.name,
                        color = cat.color,
                        icon = cat.icon,
                        isPersonal = cat.userId != null,
                        ownerId = cat.userId
                    )
                }
            }
        }

        return resultCategories.values.toList().sortedBy { it.name.lowercase() }
    }
}
