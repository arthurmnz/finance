package com.finance.infraestructure.persistence.mapper

import com.finance.domain.entity.AccountEntity
import com.finance.infraestructure.persistence.entity.AccountJpaEntity
import org.springframework.stereotype.Component

@Component
class AccountMapper {

    fun toDomain(jpaEntity: AccountJpaEntity): AccountEntity {
        return AccountEntity(
            id = jpaEntity.id,
            ownerId = jpaEntity.ownerId,
            name = jpaEntity.name,
            startBalance = jpaEntity.startBalance,
            balance = jpaEntity.balance,
            type = jpaEntity.type,
            tax = jpaEntity.tax,
            groupId = jpaEntity.groupId,
            createdAt = jpaEntity.createdAt,
            updatedAt = jpaEntity.updatedAt
        )
    }

    fun toJpaEntity(domainEntity: AccountEntity): AccountJpaEntity {
        return AccountJpaEntity(
            id = domainEntity.id,
            ownerId = domainEntity.ownerId,
            name = domainEntity.name,
            startBalance = domainEntity.startBalance,
            balance = domainEntity.balance,
            type = domainEntity.type,
            tax = domainEntity.tax,
            groupId = domainEntity.groupId,
            createdAt = domainEntity.createdAt,
            updatedAt = domainEntity.updatedAt
        )
    }
}
