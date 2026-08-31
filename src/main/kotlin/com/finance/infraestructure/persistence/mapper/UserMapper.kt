package com.finance.infraestructure.persistence.mapper

import com.finance.domain.entity.UserEntity
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import com.finance.infraestructure.persistence.entity.UserJpaEntity
import org.springframework.stereotype.Component

@Component
class UserMapper {

    fun toDomain(jpaEntity: UserJpaEntity): UserEntity {
        return UserEntity(
            id = jpaEntity.id,
            firstName = Name(jpaEntity.firstName),
            lastName = Name(jpaEntity.lastName),
            email = Email(jpaEntity.email),
            passwordHash = PasswordHash(jpaEntity.passwordHash),
            createdAt = jpaEntity.createdAt,
            updatedAt = jpaEntity.updatedAt,
        )
    }

    fun toJpaEntity(domainEntity: UserEntity): UserJpaEntity {
        return UserJpaEntity(
            id = domainEntity.id,
            firstName = domainEntity.firstName.value,
            lastName = domainEntity.lastName.value,
            email = domainEntity.email.normalized,
            passwordHash = domainEntity.passwordHash.value,
            createdAt = domainEntity.createdAt,
            updatedAt = domainEntity.updatedAt,
        )
    }
}
