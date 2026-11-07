package com.finance.infraestructure.security

import com.finance.domain.exception.RateLimitExceededException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.mock.web.MockHttpServletRequest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RateLimiterServiceTest {

    private lateinit var rateLimiterService: RateLimiterService

    @BeforeEach
    fun setup() {
        rateLimiterService = RateLimiterService(maxEntries = 5)
    }

    @Test
    fun `should allow up to 5 requests per IP and throw RateLimitExceededException on 6th`() {
        val ip = "192.168.1.100"

        repeat(5) {
            rateLimiterService.checkRateLimit(action = "test-action", clientIp = ip, maxRequests = 5, windowMinutes = 15)
        }

        assertThrows<RateLimitExceededException> {
            rateLimiterService.checkRateLimit(action = "test-action", clientIp = ip, maxRequests = 5, windowMinutes = 15)
        }
    }

    @Test
    fun `should enforce limit per destination email allowing up to 3 requests`() {
        val email = "destinatario@finance.com"

        assertTrue(rateLimiterService.tryAcquire(action = "forgot-password-email", key = email, maxRequests = 3, windowMinutes = 15))
        assertTrue(rateLimiterService.tryAcquire(action = "forgot-password-email", key = email, maxRequests = 3, windowMinutes = 15))
        assertTrue(rateLimiterService.tryAcquire(action = "forgot-password-email", key = email, maxRequests = 3, windowMinutes = 15))

        // 4ª tentativa deve ser rejeitada suavemente (retorna false)
        assertFalse(rateLimiterService.tryAcquire(action = "forgot-password-email", key = email, maxRequests = 3, windowMinutes = 15))
    }

    @Test
    fun `should ignore unvalidated X-Forwarded-For header and use remoteAddr`() {
        val request = MockHttpServletRequest()
        request.remoteAddr = "10.0.0.5"
        request.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18")

        val resolvedIp = rateLimiterService.resolveClientIp(request)
        assertEquals("10.0.0.5", resolvedIp)
    }

    @Test
    fun `should enforce memory cap evicting oldest entries when capacity is exceeded`() {
        // Criar 5 chaves diferentes (capacidade máxima é 5)
        for (i in 1..5) {
            rateLimiterService.tryAcquire(action = "act", key = "user_$i", maxRequests = 5, windowMinutes = 15)
        }
        assertEquals(5, rateLimiterService.currentEntriesCount())

        // Inserir a 6ª chave deve descartar a mais antiga (LRU) sem ultrapassar a capacidade máxima
        rateLimiterService.tryAcquire(action = "act", key = "user_6", maxRequests = 5, windowMinutes = 15)
        assertEquals(5, rateLimiterService.currentEntriesCount())
    }

    @Test
    fun `should cleanup expired timestamps and empty queues`() {
        rateLimiterService.tryAcquire(action = "act", key = "key1", maxRequests = 5, windowMinutes = 15)
        assertEquals(1, rateLimiterService.currentEntriesCount())

        // Limpeza com janela de 0 minutos expurga tudo
        rateLimiterService.cleanupExpired(windowMinutes = 0)
        assertEquals(0, rateLimiterService.currentEntriesCount())
    }
}
