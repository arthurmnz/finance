package com.finance.infraestructure.persistence.mapper

import com.finance.domain.entity.CategoryEntity
import com.finance.infraestructure.persistence.entity.CategoryJpaEntity
import org.springframework.stereotype.Component

@Component
class CategoryMapper {
    fun toDomain(jpaEntity: CategoryJpaEntity): CategoryEntity {
        return CategoryEntity(
            id = jpaEntity.id,
            name = jpaEntity.name,
            description = jpaEntity.description,
            userId = jpaEntity.userId,
            groupId = jpaEntity.groupId,
            type = jpaEntity.type,
            color = jpaEntity.color,
            icon = jpaEntity.icon,
            level = jpaEntity.level,
            parentId = jpaEntity.parentId,
            systemKey = jpaEntity.systemKey,
            isDefault = jpaEntity.isDefault,
            hierarchyMigrated = jpaEntity.hierarchyMigrated,
            createdAt = jpaEntity.createdAt,
            updatedAt = jpaEntity.updatedAt
        )
    }

    fun toJpaEntity(domainEntity: CategoryEntity): CategoryJpaEntity {
        return CategoryJpaEntity(
            id = domainEntity.id,
            name = domainEntity.name,
            description = domainEntity.description,
            userId = domainEntity.userId,
            groupId = domainEntity.groupId,
            type = domainEntity.type,
            color = domainEntity.color,
            icon = domainEntity.icon,
            level = domainEntity.level,
            parentId = domainEntity.parentId,
            systemKey = domainEntity.systemKey,
            isDefault = domainEntity.isDefault,
            hierarchyMigrated = domainEntity.hierarchyMigrated,
            createdAt = domainEntity.createdAt,
            updatedAt = domainEntity.updatedAt
        )
    }
}
