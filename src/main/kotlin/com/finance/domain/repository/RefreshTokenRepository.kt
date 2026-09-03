package com.finance.domain.repository

import com.finance.domain.entity.RefreshTokenEntity
import java.util.UUID

interface RefreshTokenRepository {
    fun save(refreshToken: RefreshTokenEntity): RefreshTokenEntity
    fun findByToken(token: String): RefreshTokenEntity?
    fun revokeByToken(token: String)
    fun revokeAllByUserId(userId: UUID)
    fun deleteByToken(token: String)
}
