package com.finance.infraestructure.persistence.repository

import com.finance.domain.entity.CreditCardGroupRequestEntity
import com.finance.domain.enum.AccountGroupRequestStatus
import com.finance.domain.repository.CreditCardGroupRequestRepository
import com.finance.infraestructure.persistence.mapper.CreditCardMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
class CreditCardGroupRequestRepositoryImpl(
    private val jpa: SpringDataCreditCardGroupRequestRepository,
    private val mapper: CreditCardMapper
) : CreditCardGroupRequestRepository {
    @Transactional
    override fun save(request: CreditCardGroupRequestEntity): CreditCardGroupRequestEntity =
        mapper.toRequestDomain(jpa.save(mapper.toRequestJpa(request)))

    @Transactional(readOnly = true)
    override fun findById(id: UUID): CreditCardGroupRequestEntity? =
        jpa.findById(id).map { mapper.toRequestDomain(it) }.orElse(null)

    @Transactional(readOnly = true)
    override fun findByCreditCardIdAndStatus(creditCardId: UUID, status: AccountGroupRequestStatus): CreditCardGroupRequestEntity? =
        jpa.findByCreditCardIdAndStatus(creditCardId, status)?.let { mapper.toRequestDomain(it) }

    @Transactional(readOnly = true)
    override fun findAllByGroupIdAndStatus(groupId: UUID, status: AccountGroupRequestStatus): List<CreditCardGroupRequestEntity> =
        jpa.findAllByGroupIdAndStatus(groupId, status).map { mapper.toRequestDomain(it) }

    @Transactional
    override fun deleteAllByCreditCardId(creditCardId: UUID) = jpa.deleteAllByCreditCardId(creditCardId)

    @Transactional
    override fun deleteAllByGroupId(groupId: UUID) = jpa.deleteAllByGroupId(groupId)
}
