package com.finance.application.use_case.category

import com.finance.application.dto.category.CreateCategoryRequest
import com.finance.application.dto.category.CategoryResponse
import com.finance.application.mapper.CategoryDtoMapper
import com.finance.domain.entity.CategoryEntity
import com.finance.domain.repository.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CreateCategoryUseCase(
    private val categoryRepository: CategoryRepository
) {
    @Transactional
    fun execute(userId: UUID, request: CreateCategoryRequest): CategoryResponse {
        val category = CategoryEntity(
            name = request.name,
            description = request.description,
            userId = if (request.groupId == null) userId else null,
            groupId = request.groupId,
            type = request.type,
            color = request.color,
            icon = request.icon
        )

        val savedCategory = categoryRepository.save(category)
        return CategoryDtoMapper.toResponse(savedCategory)
    }
}
