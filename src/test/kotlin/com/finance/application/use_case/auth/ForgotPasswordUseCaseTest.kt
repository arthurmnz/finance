package com.finance.application.use_case.auth

import com.finance.application.dto.auth.ForgotPasswordRequest
import com.finance.application.event.PasswordResetRequestedEvent
import com.finance.application.port.EmailSenderPort
import com.finance.application.service.InMemoryUserRepository
import com.finance.domain.entity.PasswordRecoveryTokenEntity
import com.finance.domain.entity.UserEntity
import com.finance.domain.repository.PasswordRecoveryTokenRepository
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import com.finance.infraestructure.security.RateLimiterService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.security.MessageDigest
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ForgotPasswordUseCaseTest {

    private lateinit var userRepository: InMemoryUserRepository
    private lateinit var tokenRepository: InMemoryPasswordRecoveryTokenRepository
    private lateinit var fakeEventPublisher: FakeEventPublisher
    private lateinit var rateLimiterService: RateLimiterService
    private lateinit var useCase: ForgotPasswordUseCase
    private lateinit var existingUser: UserEntity

    @BeforeEach
    fun setup() {
        userRepository = InMemoryUserRepository()
        tokenRepository = InMemoryPasswordRecoveryTokenRepository()
        fakeEventPublisher = FakeEventPublisher()
        rateLimiterService = RateLimiterService()

        useCase = ForgotPasswordUseCase(
            userRepository = userRepository,
            passwordRecoveryTokenRepository = tokenRepository,
            eventPublisher = fakeEventPublisher,
            rateLimiterService = rateLimiterService,
            frontendUrl = "http://localhost:5173",
        )

        existingUser = UserEntity(
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur@finance.com"),
            passwordHash = PasswordHash("hashedPassword123!"),
        )
        userRepository.save(existingUser)
    }

    @Test
    fun `should process forgot password for existing user, save token and publish async event`() {
        val request = ForgotPasswordRequest(email = "arthur@finance.com")

        val response = useCase.execute(request)

        assertEquals(ForgotPasswordUseCase.SUCCESS_MESSAGE, response.message)
        assertEquals(1, fakeEventPublisher.events.size)
        val event = fakeEventPublisher.events.first() as PasswordResetRequestedEvent
        assertEquals("arthur@finance.com", event.toEmail)
        assertEquals("Arthur", event.recipientName)
        assertTrue(event.resetLink.startsWith("http://localhost:5173/redefinir-senha?token="))

        val rawToken = event.resetLink.substringAfter("token=")
        val tokenHash = sha256(rawToken)
        val savedEntity = tokenRepository.findByTokenHash(tokenHash)
        assertNotNull(savedEntity)
        assertEquals(existingUser.id, savedEntity.userId)
        assertTrue(savedEntity.isValid)
    }

    @Test
    fun `should return same success message and not publish event if user does not exist`() {
        val request = ForgotPasswordRequest(email = "naoexiste@finance.com")

        val response = useCase.execute(request)

        assertEquals(ForgotPasswordUseCase.SUCCESS_MESSAGE, response.message)
        assertTrue(fakeEventPublisher.events.isEmpty())
        assertTrue(tokenRepository.tokens.isEmpty())
    }

    @Test
    fun `should return same success message and not publish event if email format is invalid`() {
        val request = ForgotPasswordRequest(email = "email-invalido")

        val response = useCase.execute(request)

        assertEquals(ForgotPasswordUseCase.SUCCESS_MESSAGE, response.message)
        assertTrue(fakeEventPublisher.events.isEmpty())
    }

    @Test
    fun `should revoke existing active tokens when a new request is made`() {
        useCase.execute(ForgotPasswordRequest(email = "arthur@finance.com"))
        assertEquals(1, fakeEventPublisher.events.size)
        val firstToken = (fakeEventPublisher.events[0] as PasswordResetRequestedEvent).resetLink.substringAfter("token=")
        val firstHash = sha256(firstToken)

        val firstEntity = tokenRepository.findByTokenHash(firstHash)
        assertNotNull(firstEntity)
        assertTrue(firstEntity.isValid)

        // Segundo pedido
        useCase.execute(ForgotPasswordRequest(email = "arthur@finance.com"))
        assertEquals(2, fakeEventPublisher.events.size)
        val secondToken = (fakeEventPublisher.events[1] as PasswordResetRequestedEvent).resetLink.substringAfter("token=")
        val secondHash = sha256(secondToken)

        // Primeiro deve estar revogado
        assertTrue(firstEntity.isRevoked)
        assertFalse(firstEntity.isValid)

        // Segundo deve estar válido
        val secondEntity = tokenRepository.findByTokenHash(secondHash)
        assertNotNull(secondEntity)
        assertTrue(secondEntity.isValid)
    }

    @Test
    fun `should enforce limit of 3 emails per window returning neutral success on 4th call`() {
        val request = ForgotPasswordRequest(email = "arthur@finance.com")

        // 3 requisições permitidas
        repeat(3) {
            val res = useCase.execute(request)
            assertEquals(ForgotPasswordUseCase.SUCCESS_MESSAGE, res.message)
        }
        assertEquals(3, fakeEventPublisher.events.size)

        // 4ª requisição deve retornar a mesma resposta 200 neutra, mas sem emitir novo evento
        val fourthResponse = useCase.execute(request)
        assertEquals(ForgotPasswordUseCase.SUCCESS_MESSAGE, fourthResponse.message)
        assertEquals(3, fakeEventPublisher.events.size) // Continua 3, sem disparar e-mail de spam
    }

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

class FakeEventPublisher : ApplicationEventPublisher {
    val events = mutableListOf<Any>()

    override fun publishEvent(event: Any) {
        events.add(event)
    }
}

class FakeEmailSender : EmailSenderPort {
    val sentEmails = mutableListOf<SentEmail>()
    var shouldThrow = false

    data class SentEmail(val toEmail: String, val recipientName: String, val resetLink: String)

    override fun sendPasswordResetEmail(toEmail: String, recipientName: String, resetLink: String) {
        if (shouldThrow) {
            throw RuntimeException("Falha ao comunicar com serviço de email")
        }
        sentEmails.add(SentEmail(toEmail, recipientName, resetLink))
    }
}

class InMemoryPasswordRecoveryTokenRepository : PasswordRecoveryTokenRepository {
    val tokens = mutableMapOf<UUID, PasswordRecoveryTokenEntity>()

    override fun save(token: PasswordRecoveryTokenEntity): PasswordRecoveryTokenEntity {
        tokens[token.id] = token
        return token
    }

    override fun findByTokenHash(tokenHash: String): PasswordRecoveryTokenEntity? {
        return tokens.values.firstOrNull { it.tokenHash == tokenHash }
    }

    override fun findByTokenHashWithLock(tokenHash: String): PasswordRecoveryTokenEntity? {
        return findByTokenHash(tokenHash)
    }

    override fun revokeAllActiveByUserId(userId: UUID) {
        tokens.values
            .filter { it.userId == userId && it.isValid }
            .forEach { it.revoke() }
    }
}
