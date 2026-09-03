package com.finance.infraestructure.persistence.entity

import com.finance.domain.enum.AccountType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "accounts")
class AccountJpaEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,

    @Column(name = "owner_id", nullable = false, updatable = false)
    var ownerId: UUID,

    @Column(name = "name", nullable = false, length = 100)
    var name: String,

    @Column(name = "start_balance", nullable = false)
    var startBalance: Int,

    @Column(name = "balance", nullable = false)
    var balance: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    var type: AccountType,

    @Column(name = "tax")
    var tax: Int?,

    @Column(name = "group_id")
    var groupId: UUID?,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
