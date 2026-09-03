package com.finance.infraestructure.persistence.entity

import com.finance.domain.enum.FriendshipStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "friendships",
    uniqueConstraints = [UniqueConstraint(columnNames = ["requester_id", "addressee_id"])]
)
class FriendshipJpaEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,

    @Column(name = "requester_id", nullable = false, updatable = false)
    var requesterId: UUID,

    @Column(name = "addressee_id", nullable = false, updatable = false)
    var addresseeId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: FriendshipStatus,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
