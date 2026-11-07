package com.finance.infraestructure.email

import com.finance.application.event.PasswordResetRequestedEvent
import com.finance.application.port.EmailSenderPort
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class PasswordResetEventListener(
    private val emailSenderPort: EmailSenderPort,
) {

    private val logger = LoggerFactory.getLogger(PasswordResetEventListener::class.java)

    @Async("mailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onPasswordResetRequested(event: PasswordResetRequestedEvent) {
        try {
            emailSenderPort.sendPasswordResetEmail(
                toEmail = event.toEmail,
                recipientName = event.recipientName,
                resetLink = event.resetLink,
            )
        } catch (ex: Exception) {
            logger.error("Erro assíncrono ao enviar e-mail de recuperação de senha para '{}'", event.toEmail, ex)
        }
    }
}
