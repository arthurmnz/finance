package com.finance.infraestructure.persistence.mapper

import com.finance.domain.entity.RefreshTokenEntity
import com.finance.infraestructure.persistence.entity.RefreshTokenJpaEntity
import org.springframework.stereotype.Component

@Component
class RefreshTokenMapper {

    fun toDomain(jpaEntity: RefreshTokenJpaEntity): RefreshTokenEntity {
        return RefreshTokenEntity(
            id = jpaEntity.id,
            userId = jpaEntity.userId,
            token = jpaEntity.token,
            expiresAt = jpaEntity.expiresAt,
            createdAt = jpaEntity.createdAt,
            revokedAt = jpaEntity.revokedAt,
        )
    }

    fun toJpaEntity(domainEntity: RefreshTokenEntity): RefreshTokenJpaEntity {
        return RefreshTokenJpaEntity(
            id = domainEntity.id,
            userId = domainEntity.userId,
            token = domainEntity.token,
            expiresAt = domainEntity.expiresAt,
            createdAt = domainEntity.createdAt,
            revokedAt = domainEntity.revokedAt,
        )
    }
}
