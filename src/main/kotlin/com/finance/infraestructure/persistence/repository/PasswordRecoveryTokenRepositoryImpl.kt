package com.finance.infraestructure.persistence.repository

import com.finance.domain.entity.PasswordRecoveryTokenEntity
import com.finance.domain.repository.PasswordRecoveryTokenRepository
import com.finance.infraestructure.persistence.mapper.PasswordRecoveryTokenMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
class PasswordRecoveryTokenRepositoryImpl(
    private val springDataPasswordRecoveryTokenRepository: SpringDataPasswordRecoveryTokenRepository,
    private val passwordRecoveryTokenMapper: PasswordRecoveryTokenMapper,
) : PasswordRecoveryTokenRepository {

    @Transactional
    override fun save(token: PasswordRecoveryTokenEntity): PasswordRecoveryTokenEntity {
        val jpaEntity = passwordRecoveryTokenMapper.toJpaEntity(token)
        val saved = springDataPasswordRecoveryTokenRepository.save(jpaEntity)
        return passwordRecoveryTokenMapper.toDomain(saved)
    }

    @Transactional(readOnly = true)
    override fun findByTokenHash(tokenHash: String): PasswordRecoveryTokenEntity? {
        val jpaEntity = springDataPasswordRecoveryTokenRepository.findByTokenHash(tokenHash)
        return jpaEntity?.let { passwordRecoveryTokenMapper.toDomain(it) }
    }

    @Transactional
    override fun findByTokenHashWithLock(tokenHash: String): PasswordRecoveryTokenEntity? {
        val jpaEntity = springDataPasswordRecoveryTokenRepository.findByTokenHashWithLock(tokenHash)
        return jpaEntity?.let { passwordRecoveryTokenMapper.toDomain(it) }
    }

    @Transactional
    override fun revokeAllActiveByUserId(userId: UUID) {
        springDataPasswordRecoveryTokenRepository.revokeAllActiveByUserId(userId)
    }
}
