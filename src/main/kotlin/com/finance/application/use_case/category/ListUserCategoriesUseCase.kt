package com.finance.application.use_case.category

import com.finance.application.dto.category.CategoryResponse
import com.finance.application.mapper.CategoryDtoMapper
import com.finance.domain.repository.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ListUserCategoriesUseCase(
    private val categoryRepository: CategoryRepository
) {
    @Transactional(readOnly = true)
    fun execute(userId: UUID): List<CategoryResponse> {
        val categories = categoryRepository.findByUserId(userId)
        return categories.map { CategoryDtoMapper.toResponse(it) }
    }
}
