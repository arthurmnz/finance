package com.finance.application.use_case.category

import com.finance.application.dto.category.CategoryResponse
import com.finance.application.mapper.CategoryBatchLoader
import com.finance.application.mapper.CategoryDtoMapper
import com.finance.domain.enum.CategoryLevel
import com.finance.domain.repository.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ListUserCategoriesUseCase(
    private val categoryRepository: CategoryRepository
) {
    @Transactional(readOnly = true)
    fun execute(
        userId: UUID,
        level: CategoryLevel? = null,
        parentId: UUID? = null
    ): List<CategoryResponse> {
        val categories = if (level != null) {
            categoryRepository.findByUserIdAndLevel(userId, level)
        } else {
            categoryRepository.findByUserId(userId)
        }

        val filtered = if (parentId != null) {
            categories.filter { it.parentId == parentId }
        } else {
            categories
        }

        val parentMap = CategoryBatchLoader.loadParentsForCategories(categoryRepository, filtered)

        return filtered
            .map { cat -> CategoryDtoMapper.toResponse(cat, cat.parentId?.let { parentMap[it] }) }
            .sortedBy { it.name.lowercase() }
    }
}
