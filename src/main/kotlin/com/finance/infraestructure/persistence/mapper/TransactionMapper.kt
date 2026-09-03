package com.finance.infraestructure.persistence.mapper

import com.finance.domain.entity.TransactionEntity
import com.finance.infraestructure.persistence.entity.TransactionJpaEntity
import org.springframework.stereotype.Component

@Component
class TransactionMapper {
    fun toDomain(jpaEntity: TransactionJpaEntity): TransactionEntity {
        return TransactionEntity(
            id = jpaEntity.id,
            title = jpaEntity.title,
            amount = jpaEntity.amount,
            date = jpaEntity.date,
            responsibleId = jpaEntity.responsibleId,
            accountId = jpaEntity.accountId,
            type = jpaEntity.type,
            destinationAccountId = jpaEntity.destinationAccountId,
            categoryId = jpaEntity.categoryId,
            status = jpaEntity.status,
            createdAt = jpaEntity.createdAt,
            updatedAt = jpaEntity.updatedAt
        )
    }

    fun toJpaEntity(domainEntity: TransactionEntity): TransactionJpaEntity {
        return TransactionJpaEntity(
            id = domainEntity.id,
            title = domainEntity.title,
            amount = domainEntity.amount,
            date = domainEntity.date,
            responsibleId = domainEntity.responsibleId,
            accountId = domainEntity.accountId,
            type = domainEntity.type,
            destinationAccountId = domainEntity.destinationAccountId,
            categoryId = domainEntity.categoryId,
            status = domainEntity.status,
            createdAt = domainEntity.createdAt,
            updatedAt = domainEntity.updatedAt
        )
    }
}
