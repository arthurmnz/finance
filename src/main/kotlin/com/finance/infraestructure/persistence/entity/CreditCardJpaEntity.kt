package com.finance.infraestructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "credit_cards")
class CreditCardJpaEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,

    @Column(name = "owner_id", nullable = false, updatable = false)
    var ownerId: UUID,

    @Column(name = "account_id", nullable = false)
    var accountId: UUID,

    @Column(name = "name", nullable = false, length = 100)
    var name: String,

    @Column(name = "limit_amount", nullable = false)
    var limitAmount: Int,

    @Column(name = "closing_day", nullable = false)
    var closingDay: Int,

    @Column(name = "due_day", nullable = false)
    var dueDay: Int,

    @Column(name = "group_id")
    var groupId: UUID?,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
