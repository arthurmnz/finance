package com.finance.application.use_case.category

import com.finance.domain.exception.CategoryNotFoundException
import com.finance.domain.exception.UnauthorizedException
import com.finance.domain.repository.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DeleteCategoryUseCase(
    private val categoryRepository: CategoryRepository
) {
    @Transactional
    fun execute(userId: UUID, categoryId: UUID) {
        val category = categoryRepository.findById(categoryId)
            ?: throw CategoryNotFoundException()

        if (category.userId != null && category.userId != userId) {
            throw UnauthorizedException("You can only delete your own categories")
        }

        categoryRepository.delete(categoryId)
    }
}
