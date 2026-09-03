package com.finance.infraestructure.persistence.entity

import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "transactions")
class TransactionJpaEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,

    @Column(name = "title", nullable = false, length = 150)
    var title: String,

    @Column(name = "amount", nullable = false)
    var amount: Int,

    @Column(name = "date", nullable = false)
    var date: LocalDateTime,

    @Column(name = "responsible_id", nullable = false)
    var responsibleId: UUID,

    @Column(name = "account_id", nullable = false)
    var accountId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    var type: TransactionType,

    @Column(name = "destination_account_id")
    var destinationAccountId: UUID?,

    @Column(name = "category_id")
    var categoryId: UUID?,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    var status: TransactionStatus,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
