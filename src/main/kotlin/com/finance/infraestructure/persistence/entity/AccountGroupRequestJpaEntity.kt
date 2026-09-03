package com.finance.infraestructure.persistence.entity

import com.finance.domain.enum.AccountGroupRequestStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "account_group_requests")
class AccountGroupRequestJpaEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,

    @Column(name = "account_id", nullable = false, updatable = false)
    var accountId: UUID,

    @Column(name = "group_id", nullable = false, updatable = false)
    var groupId: UUID,

    @Column(name = "requester_id", nullable = false, updatable = false)
    var requesterId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: AccountGroupRequestStatus,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
