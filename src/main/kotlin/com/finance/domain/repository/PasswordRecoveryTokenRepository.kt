package com.finance.domain.repository

import com.finance.domain.entity.PasswordRecoveryTokenEntity
import java.util.UUID

interface PasswordRecoveryTokenRepository {
    fun save(token: PasswordRecoveryTokenEntity): PasswordRecoveryTokenEntity
    fun findByTokenHash(tokenHash: String): PasswordRecoveryTokenEntity?
    fun findByTokenHashWithLock(tokenHash: String): PasswordRecoveryTokenEntity?
    fun revokeAllActiveByUserId(userId: UUID)
}
