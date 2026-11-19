package com.finance.application.use_case.category

import com.finance.application.service.GroupService
import com.finance.domain.enum.CategoryLevel
import com.finance.domain.exception.CategoryNotFoundException
import com.finance.domain.exception.InvalidCategoryException
import com.finance.domain.exception.UnauthorizedException
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DeleteCategoryUseCase(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val groupService: GroupService
) {
    @Transactional
    fun execute(userId: UUID, categoryId: UUID) {
        val category = categoryRepository.findById(categoryId)
            ?: throw CategoryNotFoundException()

        val groupId = category.groupId
        if (groupId != null) {
            val member = groupService.findMember(groupId, userId)
            groupService.requireOwnerOrAdmin(member)
        } else if (category.userId != userId) {
            throw UnauthorizedException("You can only delete your own categories")
        }

        if (categoryRepository.existsByParentId(category.id)) {
            throw InvalidCategoryException(
                "Não é possível excluir uma categoria geral que possui subcategorias."
            )
        }

        if (category.level == CategoryLevel.SPECIFIC) {
            val parentId = category.parentId
            if (parentId != null) {
                transactionRepository.fallbackSpecificCategoryToGeneral(categoryId, parentId)
            } else {
                transactionRepository.clearCategoryReferences(categoryId)
            }
        } else {
            transactionRepository.clearCategoryReferences(categoryId)
        }
        categoryRepository.delete(categoryId)
    }
}
