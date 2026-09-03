package com.finance.infraestructure.persistence.mapper

import com.finance.domain.entity.GroupInvitationEntity
import com.finance.infraestructure.persistence.entity.GroupInvitationJpaEntity
import org.springframework.stereotype.Component

@Component
class GroupInvitationMapper {

    fun toDomain(jpaEntity: GroupInvitationJpaEntity): GroupInvitationEntity {
        return GroupInvitationEntity(
            id = jpaEntity.id,
            groupId = jpaEntity.groupId,
            inviterId = jpaEntity.inviterId,
            inviteeId = jpaEntity.inviteeId,
            status = jpaEntity.status,
            createdAt = jpaEntity.createdAt,
            updatedAt = jpaEntity.updatedAt,
        )
    }

    fun toJpaEntity(domainEntity: GroupInvitationEntity): GroupInvitationJpaEntity {
        return GroupInvitationJpaEntity(
            id = domainEntity.id,
            groupId = domainEntity.groupId,
            inviterId = domainEntity.inviterId,
            inviteeId = domainEntity.inviteeId,
            status = domainEntity.status,
            createdAt = domainEntity.createdAt,
            updatedAt = domainEntity.updatedAt,
        )
    }
}
