package com.finance.infraestructure.persistence.mapper

import com.finance.domain.entity.AccountGroupRequestEntity
import com.finance.infraestructure.persistence.entity.AccountGroupRequestJpaEntity
import org.springframework.stereotype.Component

@Component
class AccountGroupRequestMapper {

    fun toDomain(jpaEntity: AccountGroupRequestJpaEntity): AccountGroupRequestEntity {
        return AccountGroupRequestEntity(
            id = jpaEntity.id,
            accountId = jpaEntity.accountId,
            groupId = jpaEntity.groupId,
            requesterId = jpaEntity.requesterId,
            status = jpaEntity.status,
            createdAt = jpaEntity.createdAt,
            updatedAt = jpaEntity.updatedAt
        )
    }

    fun toJpaEntity(domainEntity: AccountGroupRequestEntity): AccountGroupRequestJpaEntity {
        return AccountGroupRequestJpaEntity(
            id = domainEntity.id,
            accountId = domainEntity.accountId,
            groupId = domainEntity.groupId,
            requesterId = domainEntity.requesterId,
            status = domainEntity.status,
            createdAt = domainEntity.createdAt,
            updatedAt = domainEntity.updatedAt
        )
    }
}
