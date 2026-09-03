package com.finance.application.use_case.category

import com.finance.application.dto.category.UpdateCategoryRequest
import com.finance.application.dto.category.CategoryResponse
import com.finance.application.mapper.CategoryDtoMapper
import com.finance.domain.exception.CategoryNotFoundException
import com.finance.domain.exception.UnauthorizedException
import com.finance.domain.repository.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UpdateCategoryUseCase(
    private val categoryRepository: CategoryRepository
) {
    @Transactional
    fun execute(userId: UUID, categoryId: UUID, request: UpdateCategoryRequest): CategoryResponse {
        val category = categoryRepository.findById(categoryId)
            ?: throw CategoryNotFoundException()

        // Check if user has permission (simplified: must be owner)
        // Group permissions would need checking if it belongs to a group.
        if (category.userId != null && category.userId != userId) {
            throw UnauthorizedException("You can only edit your own categories")
        }

        category.update(
            newName = request.name,
            newDescription = request.description,
            newType = request.type,
            newColor = request.color,
            newIcon = request.icon
        )

        val updatedCategory = categoryRepository.save(category)
        return CategoryDtoMapper.toResponse(updatedCategory)
    }
}
