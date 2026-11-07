package com.finance.infraestructure.persistence.repository

import com.finance.infraestructure.persistence.entity.DebtSettlementJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface SpringDataDebtSettlementRepository : JpaRepository<DebtSettlementJpaEntity, UUID> {
    fun findByGroupId(groupId: UUID): List<DebtSettlementJpaEntity>

    @Query("SELECT s FROM DebtSettlementJpaEntity s WHERE s.debtorId = :userId OR s.creditorId = :userId")
    fun findByDebtorIdOrCreditorId(userId: UUID): List<DebtSettlementJpaEntity>
}
