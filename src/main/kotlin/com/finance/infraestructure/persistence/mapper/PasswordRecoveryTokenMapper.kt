package com.finance.infraestructure.persistence.mapper

import com.finance.domain.entity.PasswordRecoveryTokenEntity
import com.finance.infraestructure.persistence.entity.PasswordRecoveryTokenJpaEntity
import org.springframework.stereotype.Component

@Component
class PasswordRecoveryTokenMapper {

    fun toDomain(jpaEntity: PasswordRecoveryTokenJpaEntity): PasswordRecoveryTokenEntity {
        return PasswordRecoveryTokenEntity(
            id = jpaEntity.id,
            userId = jpaEntity.userId,
            tokenHash = jpaEntity.tokenHash,
            expiresAt = jpaEntity.expiresAt,
            createdAt = jpaEntity.createdAt,
            usedAt = jpaEntity.usedAt,
            revokedAt = jpaEntity.revokedAt,
        )
    }

    fun toJpaEntity(domainEntity: PasswordRecoveryTokenEntity): PasswordRecoveryTokenJpaEntity {
        return PasswordRecoveryTokenJpaEntity(
            id = domainEntity.id,
            userId = domainEntity.userId,
            tokenHash = domainEntity.tokenHash,
            expiresAt = domainEntity.expiresAt,
            createdAt = domainEntity.createdAt,
            usedAt = domainEntity.usedAt,
            revokedAt = domainEntity.revokedAt,
        )
    }
}
