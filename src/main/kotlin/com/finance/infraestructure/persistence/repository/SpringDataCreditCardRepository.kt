package com.finance.infraestructure.persistence.repository

import com.finance.infraestructure.persistence.entity.CreditCardJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SpringDataCreditCardRepository : JpaRepository<CreditCardJpaEntity, UUID> {
    fun findAllByOwnerId(ownerId: UUID): List<CreditCardJpaEntity>
    fun findAllByGroupId(groupId: UUID): List<CreditCardJpaEntity>
    fun findAllByGroupIdIn(groupIds: List<UUID>): List<CreditCardJpaEntity>
}
