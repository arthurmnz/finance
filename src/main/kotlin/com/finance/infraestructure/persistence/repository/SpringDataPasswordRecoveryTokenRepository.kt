package com.finance.infraestructure.persistence.repository

import com.finance.infraestructure.persistence.entity.PasswordRecoveryTokenJpaEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface SpringDataPasswordRecoveryTokenRepository : JpaRepository<PasswordRecoveryTokenJpaEntity, UUID> {

    fun findByTokenHash(tokenHash: String): PasswordRecoveryTokenJpaEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PasswordRecoveryTokenJpaEntity p WHERE p.tokenHash = :tokenHash")
    fun findByTokenHashWithLock(@Param("tokenHash") tokenHash: String): PasswordRecoveryTokenJpaEntity?

    @Modifying
    @Query(
        """
        UPDATE PasswordRecoveryTokenJpaEntity p
        SET p.revokedAt = :now
        WHERE p.userId = :userId
          AND p.revokedAt IS NULL
          AND p.usedAt IS NULL
        """
    )
    fun revokeAllActiveByUserId(
        @Param("userId") userId: UUID,
        @Param("now") now: LocalDateTime = LocalDateTime.now(),
    )

    @Modifying
    @Query("DELETE FROM PasswordRecoveryTokenJpaEntity p WHERE p.expiresAt < :threshold")
    fun deleteByExpiresAtBefore(@Param("threshold") threshold: LocalDateTime): Int
}
