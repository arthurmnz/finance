package com.finance.application.use_case.auth

import com.finance.application.dto.auth.MessageResponse
import com.finance.application.dto.auth.ResetPasswordRequest
import com.finance.application.port.PasswordEncoderPort
import com.finance.domain.repository.PasswordRecoveryTokenRepository
import com.finance.domain.repository.RefreshTokenRepository
import com.finance.domain.repository.UserRepository
import com.finance.domain.value_object.PasswordHash
import com.finance.domain.value_object.PasswordRules
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest

@Service
class ResetPasswordUseCase(
    private val passwordRecoveryTokenRepository: PasswordRecoveryTokenRepository,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoderPort,
) {

    companion object {
        const val INVALID_TOKEN_MESSAGE = "Link de recuperação inválido ou expirado."
        const val SUCCESS_MESSAGE = "Senha redefinida com sucesso. Faça login com suas novas credenciais."
    }

    @Transactional
    fun execute(request: ResetPasswordRequest): MessageResponse {
        val validPassword = PasswordRules.validate(request.newPassword)

        if (request.token.isBlank()) {
            throw IllegalArgumentException(INVALID_TOKEN_MESSAGE)
        }

        val tokenHash = hashToken(request.token)
        // Busca com lock pessimista para mitigar race conditions em concorrência
        val tokenEntity = passwordRecoveryTokenRepository.findByTokenHashWithLock(tokenHash)
            ?: throw IllegalArgumentException(INVALID_TOKEN_MESSAGE)

        // Verificação em tempo constante contra timing side-channels
        val hashMatches = MessageDigest.isEqual(
            tokenHash.toByteArray(Charsets.UTF_8),
            tokenEntity.tokenHash.toByteArray(Charsets.UTF_8)
        )

        if (!hashMatches || !tokenEntity.isValid) {
            throw IllegalArgumentException(INVALID_TOKEN_MESSAGE)
        }

        val user = userRepository.findById(tokenEntity.userId)
            ?: throw IllegalArgumentException(INVALID_TOKEN_MESSAGE)

        val hashedPassword = passwordEncoder.encode(validPassword)
        user.changePassword(PasswordHash(hashedPassword))
        userRepository.save(user)

        tokenEntity.markAsUsed()
        passwordRecoveryTokenRepository.save(tokenEntity)

        refreshTokenRepository.revokeAllByUserId(user.id)

        return MessageResponse(SUCCESS_MESSAGE)
    }

    private fun hashToken(token: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(token.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
