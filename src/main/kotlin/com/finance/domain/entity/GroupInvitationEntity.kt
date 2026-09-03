package com.finance.domain.entity

import com.finance.domain.enum.GroupInvitationStatus
import java.time.LocalDateTime
import java.util.UUID

class GroupInvitationEntity(
    val id: UUID,
    val groupId: UUID,
    val inviterId: UUID,
    val inviteeId: UUID,
    status: GroupInvitationStatus,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    var status: GroupInvitationStatus = status
        private set

    var updatedAt: LocalDateTime = updatedAt
        private set

    constructor(groupId: UUID, inviterId: UUID, inviteeId: UUID) : this(
        id = UUID.randomUUID(),
        groupId = groupId,
        inviterId = inviterId,
        inviteeId = inviteeId,
        status = GroupInvitationStatus.PENDING,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
    )

    fun accept() {
        check(status == GroupInvitationStatus.PENDING) { "Apenas convites pendentes podem ser aceitos." }
        this.status = GroupInvitationStatus.ACCEPTED
        this.updatedAt = LocalDateTime.now()
    }

    fun reject() {
        check(status == GroupInvitationStatus.PENDING) { "Apenas convites pendentes podem ser recusados." }
        this.status = GroupInvitationStatus.REJECTED
        this.updatedAt = LocalDateTime.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupInvitationEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
