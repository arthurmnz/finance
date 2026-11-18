package com.finance.application.service

import com.finance.domain.entity.CategoryEntity
import com.finance.domain.enum.CategoryLevel
import com.finance.domain.enum.CategoryType
import com.finance.domain.exception.InvalidCategoryException
import java.util.UUID

object CategoryHierarchyValidator {
    fun validateParentScopeAndCompatibility(
        parent: CategoryEntity,
        targetUserId: UUID?,
        targetGroupId: UUID?,
        childType: CategoryType
    ) {
        if (targetGroupId != null) {
            if (parent.groupId != targetGroupId) {
                throw InvalidCategoryException("A categoria pai deve pertencer ao mesmo grupo.")
            }
        } else if (parent.userId != targetUserId || parent.groupId != null) {
            throw InvalidCategoryException("A categoria pai deve pertencer ao mesmo usuário.")
        }

        if (parent.level != CategoryLevel.GENERAL) {
            throw InvalidCategoryException("Uma categoria específica só pode ser vinculada a uma categoria geral.")
        }

        if (parent.type != CategoryType.BOTH && childType != parent.type) {
            throw InvalidCategoryException(
                "O tipo da categoria específica ($childType) deve ser compatível com a categoria geral (${parent.type})."
            )
        }
    }
}
