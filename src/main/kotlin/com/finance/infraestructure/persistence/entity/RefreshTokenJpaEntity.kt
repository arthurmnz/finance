package com.finance.infraestructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "refresh_tokens",
    indexes = [
        Index(name = "idx_refresh_token_token", columnList = "token", unique = true),
        Index(name = "idx_refresh_token_user_id", columnList = "user_id"),
    ]
)
class RefreshTokenJpaEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,

    @Column(name = "user_id", nullable = false, updatable = false)
    var userId: UUID,

    @Column(name = "token", nullable = false, unique = true, length = 512)
    var token: String,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "revoked_at", nullable = true)
    var revokedAt: LocalDateTime? = null,
)
