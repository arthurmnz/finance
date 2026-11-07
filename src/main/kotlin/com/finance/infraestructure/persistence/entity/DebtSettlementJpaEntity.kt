package com.finance.infraestructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "debt_settlements")
class DebtSettlementJpaEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,

    @Column(name = "group_id", nullable = false, updatable = false)
    var groupId: UUID,

    @Column(name = "debtor_id", nullable = false, updatable = false)
    var debtorId: UUID,

    @Column(name = "creditor_id", nullable = false, updatable = false)
    var creditorId: UUID,

    @Column(name = "amount", nullable = false)
    var amount: Int,

    @Column(name = "target_month", nullable = false)
    var targetMonth: Int,

    @Column(name = "target_year", nullable = false)
    var targetYear: Int,

    @Column(name = "date", nullable = false)
    var date: LocalDate,

    @Column(name = "transaction_id", nullable = true)
    var transactionId: UUID? = null,

    @Column(name = "notes", nullable = true, length = 500)
    var notes: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
