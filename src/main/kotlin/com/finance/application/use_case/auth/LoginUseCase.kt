package com.finance.application.use_case.auth

import com.finance.application.dto.auth.AuthResult
import com.finance.application.dto.auth.LoginRequest
import com.finance.application.mapper.UserDtoMapper
import com.finance.application.port.PasswordEncoderPort
import com.finance.application.port.TokenProviderPort
import com.finance.application.service.UserService
import com.finance.domain.entity.RefreshTokenEntity
import com.finance.domain.exception.UnauthorizedException
import com.finance.domain.repository.RefreshTokenRepository
import com.finance.domain.value_object.Email
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class LoginUseCase(
    private val userService: UserService,
    private val passwordEncoder: PasswordEncoderPort,
    private val tokenProvider: TokenProviderPort,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userDtoMapper: UserDtoMapper,
) {

    @Transactional
    fun execute(request: LoginRequest): AuthResult {
        val user = try {
            userService.findByEmail(Email(request.email))
        } catch (e: Exception) {
            throw UnauthorizedException("Credenciais inválidas.")
        }

        val isPasswordValid = passwordEncoder.matches(request.password, user.passwordHash.value)
        if (!isPasswordValid) {
            throw UnauthorizedException("Credenciais inválidas.")
        }

        val accessToken = tokenProvider.generateAccessToken(user)
        val refreshTokenValue = tokenProvider.generateRefreshToken()
        val expiresAt = LocalDateTime.now().plus(tokenProvider.getRefreshTokenDuration())

        val refreshTokenEntity = RefreshTokenEntity(
            userId = user.id,
            token = refreshTokenValue,
            expiresAt = expiresAt,
        )
        refreshTokenRepository.save(refreshTokenEntity)

        return AuthResult(
            accessToken = accessToken,
            refreshToken = refreshTokenValue,
            expiresIn = tokenProvider.getAccessTokenExpirationSeconds(),
            user = userDtoMapper.toUserResponse(user),
        )
    }
}
