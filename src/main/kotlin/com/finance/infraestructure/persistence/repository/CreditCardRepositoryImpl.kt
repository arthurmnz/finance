package com.finance.infraestructure.persistence.repository

import com.finance.domain.entity.CreditCardEntity
import com.finance.domain.repository.CreditCardRepository
import com.finance.infraestructure.persistence.mapper.CreditCardMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
class CreditCardRepositoryImpl(
    private val jpa: SpringDataCreditCardRepository,
    private val mapper: CreditCardMapper
) : CreditCardRepository {
    @Transactional
    override fun save(card: CreditCardEntity): CreditCardEntity = mapper.toDomain(jpa.save(mapper.toJpaEntity(card)))

    @Transactional(readOnly = true)
    override fun findById(id: UUID): CreditCardEntity? = jpa.findById(id).map { mapper.toDomain(it) }.orElse(null)

    @Transactional(readOnly = true)
    override fun findAllByOwnerId(ownerId: UUID): List<CreditCardEntity> = jpa.findAllByOwnerId(ownerId).map { mapper.toDomain(it) }

    @Transactional(readOnly = true)
    override fun findAllByGroupId(groupId: UUID): List<CreditCardEntity> = jpa.findAllByGroupId(groupId).map { mapper.toDomain(it) }

    @Transactional(readOnly = true)
    override fun findAllByGroupIdIn(groupIds: List<UUID>): List<CreditCardEntity> =
        if (groupIds.isEmpty()) emptyList() else jpa.findAllByGroupIdIn(groupIds).map { mapper.toDomain(it) }

    @Transactional
    override fun deleteById(id: UUID) = jpa.deleteById(id)
}
