package com.finance.application.use_case.auth

import com.finance.application.dto.auth.AuthResult
import com.finance.application.mapper.UserDtoMapper
import com.finance.application.port.TokenProviderPort
import com.finance.application.service.UserService
import com.finance.domain.entity.RefreshTokenEntity
import com.finance.domain.exception.UnauthorizedException
import com.finance.domain.repository.RefreshTokenRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class RefreshTokenUseCase(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userService: UserService,
    private val tokenProvider: TokenProviderPort,
    private val userDtoMapper: UserDtoMapper,
) {

    @Transactional
    fun execute(rawRefreshToken: String?): AuthResult {
        if (rawRefreshToken.isNullOrBlank()) {
            throw UnauthorizedException("Refresh token não informado.")
        }

        val existingToken = refreshTokenRepository.findByToken(rawRefreshToken)
            ?: throw UnauthorizedException("Refresh token inválido.")

        if (existingToken.isRevoked) {
            refreshTokenRepository.revokeAllByUserId(existingToken.userId)
            throw UnauthorizedException("Tentativa de reutilização de refresh token detectada.")
        }

        if (existingToken.isExpired) {
            throw UnauthorizedException("Refresh token expirado.")
        }

        existingToken.revoke()
        refreshTokenRepository.save(existingToken)

        val user = userService.findById(existingToken.userId)

        val newAccessToken = tokenProvider.generateAccessToken(user)
        val newRefreshTokenValue = tokenProvider.generateRefreshToken()
        val newExpiresAt = LocalDateTime.now().plus(tokenProvider.getRefreshTokenDuration())

        val newRefreshTokenEntity = RefreshTokenEntity(
            userId = user.id,
            token = newRefreshTokenValue,
            expiresAt = newExpiresAt,
        )
        refreshTokenRepository.save(newRefreshTokenEntity)

        return AuthResult(
            accessToken = newAccessToken,
            refreshToken = newRefreshTokenValue,
            expiresIn = tokenProvider.getAccessTokenExpirationSeconds(),
            user = userDtoMapper.toUserResponse(user),
        )
    }
}
