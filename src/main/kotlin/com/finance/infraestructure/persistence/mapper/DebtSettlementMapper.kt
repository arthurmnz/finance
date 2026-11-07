package com.finance.infraestructure.persistence.mapper

import com.finance.domain.entity.DebtSettlementEntity
import com.finance.infraestructure.persistence.entity.DebtSettlementJpaEntity
import org.springframework.stereotype.Component

@Component
class DebtSettlementMapper {
    fun toDomain(entity: DebtSettlementJpaEntity): DebtSettlementEntity =
        DebtSettlementEntity(
            id = entity.id,
            groupId = entity.groupId,
            debtorId = entity.debtorId,
            creditorId = entity.creditorId,
            amount = entity.amount,
            targetMonth = entity.targetMonth,
            targetYear = entity.targetYear,
            date = entity.date,
            transactionId = entity.transactionId,
            notes = entity.notes,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )

    fun toJpaEntity(domain: DebtSettlementEntity): DebtSettlementJpaEntity =
        DebtSettlementJpaEntity(
            id = domain.id,
            groupId = domain.groupId,
            debtorId = domain.debtorId,
            creditorId = domain.creditorId,
            amount = domain.amount,
            targetMonth = domain.targetMonth,
            targetYear = domain.targetYear,
            date = domain.date,
            transactionId = domain.transactionId,
            notes = domain.notes,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
}
