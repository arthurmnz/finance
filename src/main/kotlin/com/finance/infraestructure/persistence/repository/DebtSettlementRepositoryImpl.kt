package com.finance.infraestructure.persistence.repository

import com.finance.domain.entity.DebtSettlementEntity
import com.finance.domain.repository.DebtSettlementRepository
import com.finance.infraestructure.persistence.mapper.DebtSettlementMapper
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class DebtSettlementRepositoryImpl(
    private val springDataRepository: SpringDataDebtSettlementRepository,
    private val mapper: DebtSettlementMapper
) : DebtSettlementRepository {

    override fun save(settlement: DebtSettlementEntity): DebtSettlementEntity {
        val jpa = mapper.toJpaEntity(settlement)
        return mapper.toDomain(springDataRepository.save(jpa))
    }

    override fun findById(id: UUID): DebtSettlementEntity? {
        return springDataRepository.findById(id).map { mapper.toDomain(it) }.orElse(null)
    }

    override fun findByGroupId(groupId: UUID): List<DebtSettlementEntity> {
        return springDataRepository.findByGroupId(groupId).map { mapper.toDomain(it) }
    }

    override fun findByDebtorIdOrCreditorId(userId: UUID): List<DebtSettlementEntity> {
        return springDataRepository.findByDebtorIdOrCreditorId(userId).map { mapper.toDomain(it) }
    }

    override fun delete(id: UUID) {
        springDataRepository.deleteById(id)
    }
}
