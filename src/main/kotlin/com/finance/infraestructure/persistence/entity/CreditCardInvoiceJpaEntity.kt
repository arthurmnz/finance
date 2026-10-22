package com.finance.infraestructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "credit_card_invoices")
class CreditCardInvoiceJpaEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,

    @Column(name = "credit_card_id", nullable = false, updatable = false)
    var creditCardId: UUID,

    @Column(name = "month", nullable = false, updatable = false)
    var month: Int,

    @Column(name = "year", nullable = false, updatable = false)
    var year: Int,

    @Column(name = "paid_amount", nullable = false)
    var paidAmount: Int = 0,

    @Column(name = "carried_over_amount", nullable = false)
    var carriedOverAmount: Int = 0,

    @Column(name = "revolving_interest_amount", nullable = false)
    var revolvingInterestAmount: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
