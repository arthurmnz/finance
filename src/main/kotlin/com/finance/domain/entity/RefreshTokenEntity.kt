package com.finance.domain.entity

import java.time.LocalDateTime
import java.util.UUID

class RefreshTokenEntity(
    val id: UUID,
    val userId: UUID,
    val token: String,
    val expiresAt: LocalDateTime,
    createdAt: LocalDateTime = LocalDateTime.now(),
    revokedAt: LocalDateTime? = null,
) {
    val createdAt: LocalDateTime = createdAt

    var revokedAt: LocalDateTime? = revokedAt
        private set

    val isRevoked: Boolean
        get() = revokedAt != null

    val isExpired: Boolean
        get() = LocalDateTime.now().isAfter(expiresAt)

    val isValid: Boolean
        get() = !isRevoked && !isExpired

    constructor(
        userId: UUID,
        token: String,
        expiresAt: LocalDateTime,
    ) : this(
        id = UUID.randomUUID(),
        userId = userId,
        token = token,
        expiresAt = expiresAt,
        createdAt = LocalDateTime.now(),
        revokedAt = null,
    )

    fun revoke() {
        if (this.revokedAt == null) {
            this.revokedAt = LocalDateTime.now()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RefreshTokenEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "RefreshTokenEntity(id=$id, userId=$userId, expiresAt=$expiresAt, isRevoked=$isRevoked)"
    }
}
