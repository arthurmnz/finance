package com.finance.domain.repository

import com.finance.domain.entity.DebtSettlementEntity
import java.util.UUID

interface DebtSettlementRepository {
    fun save(settlement: DebtSettlementEntity): DebtSettlementEntity
    fun findById(id: UUID): DebtSettlementEntity?
    fun findByGroupId(groupId: UUID): List<DebtSettlementEntity>
    fun findByDebtorIdOrCreditorId(userId: UUID): List<DebtSettlementEntity>
    fun delete(id: UUID)
}
