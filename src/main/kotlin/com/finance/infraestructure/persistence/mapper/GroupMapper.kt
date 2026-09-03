package com.finance.infraestructure.persistence.mapper

import com.finance.domain.entity.GroupEntity
import com.finance.infraestructure.persistence.entity.GroupJpaEntity
import org.springframework.stereotype.Component

@Component
class GroupMapper {

    fun toDomain(jpaEntity: GroupJpaEntity): GroupEntity {
        return GroupEntity(
            id = jpaEntity.id,
            name = jpaEntity.name,
            createdAt = jpaEntity.createdAt,
            updatedAt = jpaEntity.updatedAt,
        )
    }

    fun toJpaEntity(domainEntity: GroupEntity): GroupJpaEntity {
        return GroupJpaEntity(
            id = domainEntity.id,
            name = domainEntity.name,
            createdAt = domainEntity.createdAt,
            updatedAt = domainEntity.updatedAt,
        )
    }
}
