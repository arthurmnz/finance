package com.finance.infraestructure.persistence.entity

import com.finance.domain.enum.GroupRole
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
    name = "group_members",
    uniqueConstraints = [UniqueConstraint(columnNames = ["group_id", "user_id"])],
)
class GroupMemberJpaEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,

    @Column(name = "group_id", nullable = false, updatable = false)
    var groupId: UUID,

    @Column(name = "user_id", nullable = false, updatable = false)
    var userId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    var role: GroupRole,

    @Column(name = "joined_at", nullable = false, updatable = false)
    var joinedAt: LocalDateTime = LocalDateTime.now(),
)
