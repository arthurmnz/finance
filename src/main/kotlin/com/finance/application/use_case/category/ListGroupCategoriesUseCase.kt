package com.finance.application.use_case.category

import com.finance.application.dto.category.CategoryResponse
import com.finance.application.mapper.CategoryDtoMapper
import com.finance.domain.repository.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ListGroupCategoriesUseCase(
    private val categoryRepository: CategoryRepository
) {
    @Transactional(readOnly = true)
    fun execute(groupId: UUID): List<CategoryResponse> {
        val categories = categoryRepository.findByGroupId(groupId)
        return categories.map { CategoryDtoMapper.toResponse(it) }
    }
}
