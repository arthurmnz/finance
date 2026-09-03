package com.finance.domain.entity

import com.finance.domain.enum.FriendshipStatus
import java.time.LocalDateTime
import java.util.UUID

class FriendshipEntity(
    val id: UUID,
    val requesterId: UUID,
    val addresseeId: UUID,
    status: FriendshipStatus,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    var status: FriendshipStatus = status
        private set

    var updatedAt: LocalDateTime = updatedAt
        private set

    constructor(
        requesterId: UUID,
        addresseeId: UUID,
    ) : this(
        id = UUID.randomUUID(),
        requesterId = requesterId,
        addresseeId = addresseeId,
        status = FriendshipStatus.PENDING,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
    )

    fun accept() {
        check(status == FriendshipStatus.PENDING) { "Apenas solicitações pendentes podem ser aceitas." }
        this.status = FriendshipStatus.ACCEPTED
        this.updatedAt = LocalDateTime.now()
    }

    fun reject() {
        check(status == FriendshipStatus.PENDING) { "Apenas solicitações pendentes podem ser rejeitadas." }
        this.status = FriendshipStatus.REJECTED
        this.updatedAt = LocalDateTime.now()
    }

    fun block() {
        check(status != FriendshipStatus.BLOCKED) { "Esta relação já está bloqueada." }
        this.status = FriendshipStatus.BLOCKED
        this.updatedAt = LocalDateTime.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FriendshipEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "FriendshipEntity(id=$id, requesterId=$requesterId, addresseeId=$addresseeId, status=$status, createdAt=$createdAt, updatedAt=$updatedAt)"
    }
}
