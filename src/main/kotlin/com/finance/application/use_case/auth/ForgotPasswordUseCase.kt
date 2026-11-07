package com.finance.application.use_case.auth

import com.finance.application.dto.auth.ForgotPasswordRequest
import com.finance.application.dto.auth.MessageResponse
import com.finance.application.event.PasswordResetRequestedEvent
import com.finance.domain.entity.PasswordRecoveryTokenEntity
import com.finance.domain.repository.PasswordRecoveryTokenRepository
import com.finance.domain.repository.UserRepository
import com.finance.domain.value_object.Email
import com.finance.infraestructure.security.RateLimiterService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.Base64

@Service
class ForgotPasswordUseCase(
    private val userRepository: UserRepository,
    private val passwordRecoveryTokenRepository: PasswordRecoveryTokenRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val rateLimiterService: RateLimiterService,
    @Value("\${frontend.url:http://localhost:5173}")
    private val frontendUrl: String = "http://localhost:5173",
) {

    private val secureRandom = SecureRandom()

    companion object {
        const val SUCCESS_MESSAGE =
            "Se o e-mail informado estiver cadastrado em nossa base, você receberá um link para redefinição de senha em instantes."
        const val MAX_EMAILS_PER_WINDOW = 3
        const val WINDOW_MINUTES = 15L
    }

    @Transactional
    fun execute(request: ForgotPasswordRequest): MessageResponse {
        val email = runCatching { Email(request.email) }.getOrNull()

        if (email == null) {
            simulateWorkload()
            return MessageResponse(SUCCESS_MESSAGE)
        }

        // Limite de envio por e-mail de destino (máx. 3 a cada 15 min)
        val emailAllowed = rateLimiterService.tryAcquire(
            action = "forgot-password-email",
            key = email.normalized,
            maxRequests = MAX_EMAILS_PER_WINDOW,
            windowMinutes = WINDOW_MINUTES,
        )

        if (!emailAllowed) {
            simulateWorkload()
            return MessageResponse(SUCCESS_MESSAGE)
        }

        val user = userRepository.findByEmail(email)
        if (user == null) {
            simulateWorkload()
            return MessageResponse(SUCCESS_MESSAGE)
        }

        passwordRecoveryTokenRepository.revokeAllActiveByUserId(user.id)

        val rawToken = generateSecureToken()
        val tokenHash = hashToken(rawToken)

        val tokenEntity = PasswordRecoveryTokenEntity(
            userId = user.id,
            tokenHash = tokenHash,
            expiresAt = LocalDateTime.now().plusMinutes(30),
        )
        passwordRecoveryTokenRepository.save(tokenEntity)

        val cleanFrontendUrl = frontendUrl.trim().trimEnd('/')
        val resetLink = "$cleanFrontendUrl/redefinir-senha?token=$rawToken"

        // Publica evento para envio 100% assíncrono após o commit da transação
        eventPublisher.publishEvent(
            PasswordResetRequestedEvent(
                toEmail = user.email.value,
                recipientName = user.firstName.value,
                resetLink = resetLink,
            )
        )

        return MessageResponse(SUCCESS_MESSAGE)
    }

    private fun generateSecureToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hashToken(token: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(token.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun simulateWorkload() {
        val dummy = ByteArray(32)
        secureRandom.nextBytes(dummy)
        val md = MessageDigest.getInstance("SHA-256")
        md.digest(dummy)
    }
}
