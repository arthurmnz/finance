package com.finance.infraestructure.persistence.repository

import com.finance.infraestructure.persistence.entity.RefreshTokenJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface SpringDataRefreshTokenRepository : JpaRepository<RefreshTokenJpaEntity, UUID> {

    fun findByToken(token: String): RefreshTokenJpaEntity?

    fun deleteByToken(token: String)

    @Modifying
    @Query("UPDATE RefreshTokenJpaEntity r SET r.revokedAt = :now WHERE r.userId = :userId AND r.revokedAt IS NULL")
    fun revokeAllByUserId(@Param("userId") userId: UUID, @Param("now") now: LocalDateTime = LocalDateTime.now())

    @Modifying
    @Query("UPDATE RefreshTokenJpaEntity r SET r.revokedAt = :now WHERE r.token = :token AND r.revokedAt IS NULL")
    fun revokeByToken(@Param("token") token: String, @Param("now") now: LocalDateTime = LocalDateTime.now())
}
