package com.finance.domain.entity

import java.time.LocalDateTime
import java.util.UUID

class PasswordRecoveryTokenEntity(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val tokenHash: String,
    val expiresAt: LocalDateTime,
    createdAt: LocalDateTime = LocalDateTime.now(),
    usedAt: LocalDateTime? = null,
    revokedAt: LocalDateTime? = null,
) {
    val createdAt: LocalDateTime = createdAt

    var usedAt: LocalDateTime? = usedAt
        private set

    var revokedAt: LocalDateTime? = revokedAt
        private set

    val isUsed: Boolean
        get() = usedAt != null

    val isRevoked: Boolean
        get() = revokedAt != null

    val isExpired: Boolean
        get() = LocalDateTime.now().isAfter(expiresAt)

    val isValid: Boolean
        get() = !isUsed && !isRevoked && !isExpired

    fun markAsUsed() {
        if (this.usedAt == null) {
            this.usedAt = LocalDateTime.now()
        }
    }

    fun revoke() {
        if (this.revokedAt == null) {
            this.revokedAt = LocalDateTime.now()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PasswordRecoveryTokenEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "PasswordRecoveryTokenEntity(id=$id, userId=$userId, expiresAt=$expiresAt, isUsed=$isUsed, isRevoked=$isRevoked)"
    }
}
