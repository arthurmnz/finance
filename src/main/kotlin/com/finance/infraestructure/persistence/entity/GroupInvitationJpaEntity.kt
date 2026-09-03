package com.finance.infraestructure.persistence.entity

import com.finance.domain.enum.GroupInvitationStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "group_invitations")
class GroupInvitationJpaEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,

    @Column(name = "group_id", nullable = false, updatable = false)
    var groupId: UUID,

    @Column(name = "inviter_id", nullable = false, updatable = false)
    var inviterId: UUID,

    @Column(name = "invitee_id", nullable = false, updatable = false)
    var inviteeId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: GroupInvitationStatus,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
