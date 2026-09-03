package com.finance.application.use_case.auth

import com.finance.domain.repository.RefreshTokenRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LogoutUseCase(
    private val refreshTokenRepository: RefreshTokenRepository,
) {

    @Transactional
    fun execute(rawRefreshToken: String?) {
        if (!rawRefreshToken.isNullOrBlank()) {
            refreshTokenRepository.revokeByToken(rawRefreshToken)
        }
    }
}
