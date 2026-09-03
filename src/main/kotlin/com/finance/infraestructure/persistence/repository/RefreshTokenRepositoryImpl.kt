package com.finance.infraestructure.persistence.repository

import com.finance.domain.entity.RefreshTokenEntity
import com.finance.domain.repository.RefreshTokenRepository
import com.finance.infraestructure.persistence.mapper.RefreshTokenMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
class RefreshTokenRepositoryImpl(
    private val springDataRefreshTokenRepository: SpringDataRefreshTokenRepository,
    private val refreshTokenMapper: RefreshTokenMapper,
) : RefreshTokenRepository {

    @Transactional
    override fun save(refreshToken: RefreshTokenEntity): RefreshTokenEntity {
        val jpaEntity = refreshTokenMapper.toJpaEntity(refreshToken)
        val saved = springDataRefreshTokenRepository.save(jpaEntity)
        return refreshTokenMapper.toDomain(saved)
    }

    @Transactional(readOnly = true)
    override fun findByToken(token: String): RefreshTokenEntity? {
        val jpaEntity = springDataRefreshTokenRepository.findByToken(token)
        return jpaEntity?.let { refreshTokenMapper.toDomain(it) }
    }

    @Transactional
    override fun revokeByToken(token: String) {
        springDataRefreshTokenRepository.revokeByToken(token)
    }

    @Transactional
    override fun revokeAllByUserId(userId: UUID) {
        springDataRefreshTokenRepository.revokeAllByUserId(userId)
    }

    @Transactional
    override fun deleteByToken(token: String) {
        springDataRefreshTokenRepository.deleteByToken(token)
    }
}
