package com.finance.application.use_case.category

import com.finance.application.dto.category.CategoryResponse
import com.finance.application.dto.category.UpdateCategoryRequest
import com.finance.application.mapper.CategoryDtoMapper
import com.finance.application.service.CategoryHierarchyValidator
import com.finance.application.service.GroupService
import com.finance.application.service.TransactionCategoryValidator
import com.finance.domain.entity.CategoryEntity
import com.finance.domain.enum.CategoryLevel
import com.finance.domain.enum.CategoryType
import com.finance.domain.exception.CategoryNotFoundException
import com.finance.domain.exception.InvalidCategoryException
import com.finance.domain.exception.UnauthorizedException
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UpdateCategoryUseCase(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val groupService: GroupService
) {
    @Transactional
    fun execute(userId: UUID, categoryId: UUID, request: UpdateCategoryRequest): CategoryResponse {
        val category = categoryRepository.findById(categoryId)
            ?: throw CategoryNotFoundException()

        val groupId = category.groupId
        if (groupId != null) {
            val member = groupService.findMember(groupId, userId)
            groupService.requireOwnerOrAdmin(member)
        } else if (category.userId != userId) {
            throw UnauthorizedException("You can only edit your own categories")
        }

        val targetLevel = request.level
            ?: if (request.parentId != null) CategoryLevel.SPECIFIC else category.level
        val targetParentId = when {
            request.level == CategoryLevel.GENERAL -> request.parentId
            request.parentId != null -> request.parentId
            targetLevel == CategoryLevel.SPECIFIC -> category.parentId
            else -> null
        }

        if (category.level == CategoryLevel.GENERAL &&
            targetLevel == CategoryLevel.SPECIFIC &&
            categoryRepository.existsByParentId(category.id)
        ) {
            throw InvalidCategoryException(
                "Não é possível converter em específica uma categoria geral que possui subcategorias."
            )
        }

        var parentCategory: CategoryEntity? = null
        when (targetLevel) {
            CategoryLevel.GENERAL -> {
                if (targetParentId != null) {
                    throw InvalidCategoryException("Categorias gerais não podem possuir categoria pai.")
                }
                if (category.level == CategoryLevel.SPECIFIC &&
                    transactionRepository.findByCategoryId(category.id).isNotEmpty()
                ) {
                    throw InvalidCategoryException(
                        "Não é possível converter uma categoria específica que já possui transações em categoria geral."
                    )
                }
                if (request.type != category.type) {
                    val children = categoryRepository.findByParentId(category.id)
                    if (request.type != CategoryType.BOTH && children.any { it.type != request.type }) {
                        throw InvalidCategoryException(
                            "Não é possível alterar o tipo da categoria geral porque existem subcategorias com tipo incompatível."
                        )
                    }
                }
            }
            CategoryLevel.SPECIFIC -> {
                val parentId = targetParentId
                    ?: throw InvalidCategoryException("Categorias específicas devem pertencer a uma categoria geral.")
                if (parentId == category.id) {
                    throw InvalidCategoryException("Uma categoria não pode ser pai de si mesma.")
                }
                val parent = categoryRepository.findById(parentId)
                    ?: throw CategoryNotFoundException("Categoria pai não encontrada.")

                CategoryHierarchyValidator.validateParentScopeAndCompatibility(
                    parent = parent,
                    targetUserId = category.userId,
                    targetGroupId = groupId,
                    childType = request.type
                )

                if (request.type != category.type && request.type != CategoryType.BOTH) {
                    val linkedTransactions = transactionRepository.findByCategoryId(category.id)
                    val hasIncompatibleTx = linkedTransactions.any { tx ->
                        !TransactionCategoryValidator.isTypeCompatible(request.type, tx.type)
                    }
                    if (hasIncompatibleTx) {
                        throw InvalidCategoryException(
                            "Não é possível alterar o tipo da categoria porque existem transações incompatíveis vinculadas."
                        )
                    }
                }

                parentCategory = parent
            }
        }

        category.update(
            newName = request.name,
            newDescription = request.description,
            newType = request.type,
            newColor = request.color,
            newIcon = request.icon,
            newLevel = targetLevel,
            newParentId = targetParentId
        )

        val updatedCategory = categoryRepository.save(category)
        return CategoryDtoMapper.toResponse(updatedCategory, parentCategory)
    }
}
