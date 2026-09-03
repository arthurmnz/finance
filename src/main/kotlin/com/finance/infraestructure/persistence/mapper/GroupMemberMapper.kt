package com.finance.infraestructure.persistence.mapper

import com.finance.domain.entity.GroupMemberEntity
import com.finance.infraestructure.persistence.entity.GroupMemberJpaEntity
import org.springframework.stereotype.Component

@Component
class GroupMemberMapper {

    fun toDomain(jpaEntity: GroupMemberJpaEntity): GroupMemberEntity {
        return GroupMemberEntity(
            id = jpaEntity.id,
            groupId = jpaEntity.groupId,
            userId = jpaEntity.userId,
            role = jpaEntity.role,
            joinedAt = jpaEntity.joinedAt,
        )
    }

    fun toJpaEntity(domainEntity: GroupMemberEntity): GroupMemberJpaEntity {
        return GroupMemberJpaEntity(
            id = domainEntity.id,
            groupId = domainEntity.groupId,
            userId = domainEntity.userId,
            role = domainEntity.role,
            joinedAt = domainEntity.joinedAt,
        )
    }
}
