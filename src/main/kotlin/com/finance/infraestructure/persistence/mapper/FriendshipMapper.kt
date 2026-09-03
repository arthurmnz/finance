package com.finance.infraestructure.persistence.mapper

import com.finance.domain.entity.FriendshipEntity
import com.finance.infraestructure.persistence.entity.FriendshipJpaEntity
import org.springframework.stereotype.Component

@Component
class FriendshipMapper {

    fun toDomain(jpaEntity: FriendshipJpaEntity): FriendshipEntity {
        return FriendshipEntity(
            id = jpaEntity.id,
            requesterId = jpaEntity.requesterId,
            addresseeId = jpaEntity.addresseeId,
            status = jpaEntity.status,
            createdAt = jpaEntity.createdAt,
            updatedAt = jpaEntity.updatedAt,
        )
    }

    fun toJpaEntity(domainEntity: FriendshipEntity): FriendshipJpaEntity {
        return FriendshipJpaEntity(
            id = domainEntity.id,
            requesterId = domainEntity.requesterId,
            addresseeId = domainEntity.addresseeId,
            status = domainEntity.status,
            createdAt = domainEntity.createdAt,
            updatedAt = domainEntity.updatedAt,
        )
    }
}
