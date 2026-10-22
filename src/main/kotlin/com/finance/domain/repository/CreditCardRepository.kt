package com.finance.domain.repository

import com.finance.domain.entity.CreditCardEntity
import java.util.UUID

interface CreditCardRepository {
    fun save(card: CreditCardEntity): CreditCardEntity
    fun findById(id: UUID): CreditCardEntity?
    fun findAllByOwnerId(ownerId: UUID): List<CreditCardEntity>
    fun findAllByGroupId(groupId: UUID): List<CreditCardEntity>
    fun findAllByGroupIdIn(groupIds: List<UUID>): List<CreditCardEntity>
    fun deleteById(id: UUID)
}
