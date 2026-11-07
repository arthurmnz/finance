package com.finance.infraestructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "password_recovery_tokens",
    indexes = [
        Index(name = "idx_password_recovery_token_hash", columnList = "token_hash", unique = true),
        Index(name = "idx_password_recovery_user_id", columnList = "user_id"),
    ]
)
class PasswordRecoveryTokenJpaEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,

    @Column(name = "user_id", nullable = false, updatable = false)
    var userId: UUID,

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    var tokenHash: String,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "used_at", nullable = true)
    var usedAt: LocalDateTime? = null,

    @Column(name = "revoked_at", nullable = true)
    var revokedAt: LocalDateTime? = null,

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0,
)
