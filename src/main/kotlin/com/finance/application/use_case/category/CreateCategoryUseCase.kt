package com.finance.application.use_case.category

import com.finance.application.dto.category.CategoryResponse
import com.finance.application.dto.category.CreateCategoryRequest
import com.finance.application.mapper.CategoryDtoMapper
import com.finance.application.service.CategoryHierarchyValidator
import com.finance.application.service.GroupService
import com.finance.domain.entity.CategoryEntity
import com.finance.domain.enum.CategoryLevel
import com.finance.domain.exception.CategoryNotFoundException
import com.finance.domain.exception.InvalidCategoryException
import com.finance.domain.repository.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CreateCategoryUseCase(
    private val categoryRepository: CategoryRepository,
    private val groupService: GroupService
) {
    @Transactional
    fun execute(userId: UUID, request: CreateCategoryRequest): CategoryResponse {
        val groupId = request.groupId
        if (groupId != null) {
            groupService.findGroupById(groupId)
            val member = groupService.findMember(groupId, userId)
            groupService.requireOwnerOrAdmin(member)
        }

        val resolvedLevel = request.level
            ?: if (request.parentId != null) CategoryLevel.SPECIFIC else CategoryLevel.GENERAL

        val parentCategory = when (resolvedLevel) {
            CategoryLevel.GENERAL -> {
                if (request.parentId != null) {
                    throw InvalidCategoryException("Categorias gerais não podem possuir categoria pai.")
                }
                null
            }
            CategoryLevel.SPECIFIC -> {
                val parentId = request.parentId
                    ?: throw InvalidCategoryException("Categorias específicas devem pertencer a uma categoria geral.")
                val parent = categoryRepository.findById(parentId)
                    ?: throw CategoryNotFoundException("Categoria pai não encontrada.")

                CategoryHierarchyValidator.validateParentScopeAndCompatibility(
                    parent = parent,
                    targetUserId = if (groupId == null) userId else null,
                    targetGroupId = groupId,
                    childType = request.type
                )
                parent
            }
        }

        val category = CategoryEntity(
            name = request.name,
            description = request.description,
            userId = if (groupId == null) userId else null,
            groupId = groupId,
            type = request.type,
            color = request.color,
            icon = request.icon,
            level = resolvedLevel,
            parentId = parentCategory?.id
        )

        val savedCategory = categoryRepository.save(category)
        return CategoryDtoMapper.toResponse(savedCategory, parentCategory)
    }
}
