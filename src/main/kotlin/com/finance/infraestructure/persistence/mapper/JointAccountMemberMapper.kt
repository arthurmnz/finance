package com.finance.infraestructure.persistence.mapper

import com.finance.domain.entity.JointAccountMemberEntity
import com.finance.infraestructure.persistence.entity.JointAccountMemberJpaEntity
import org.springframework.stereotype.Component

@Component
class JointAccountMemberMapper {

    fun toDomain(jpaEntity: JointAccountMemberJpaEntity): JointAccountMemberEntity {
        return JointAccountMemberEntity(
            id = jpaEntity.id,
            accountId = jpaEntity.accountId,
            userId = jpaEntity.userId,
            createdAt = jpaEntity.createdAt,
        )
    }

    fun toJpaEntity(domainEntity: JointAccountMemberEntity): JointAccountMemberJpaEntity {
        return JointAccountMemberJpaEntity(
            id = domainEntity.id,
            accountId = domainEntity.accountId,
            userId = domainEntity.userId,
            createdAt = domainEntity.createdAt,
        )
    }
}
