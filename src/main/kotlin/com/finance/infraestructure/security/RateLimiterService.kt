package com.finance.infraestructure.security

import com.finance.domain.exception.RateLimitExceededException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.Collections
import java.util.concurrent.ConcurrentLinkedDeque

@Service
class RateLimiterService(
    private val maxEntries: Int = 10_000,
) {

    private val requestLog = Collections.synchronizedMap(
        object : LinkedHashMap<String, ConcurrentLinkedDeque<Instant>>(maxEntries, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ConcurrentLinkedDeque<Instant>>?): Boolean {
                return size > maxEntries
            }
        }
    )

    fun checkRateLimit(
        action: String,
        clientIp: String,
        maxRequests: Int = 5,
        windowMinutes: Long = 15,
    ) {
        val allowed = tryAcquire(
            action = action,
            key = clientIp,
            maxRequests = maxRequests,
            windowMinutes = windowMinutes,
        )

        if (!allowed) {
            throw RateLimitExceededException(
                "Muitas tentativas de recuperação de senha. Limite de $maxRequests requisições a cada $windowMinutes minutos excedido."
            )
        }
    }

    fun tryAcquire(
        action: String,
        key: String,
        maxRequests: Int,
        windowMinutes: Long = 15,
    ): Boolean {
        val compositeKey = "$action:$key"
        val now = Instant.now()
        val cutoff = now.minus(Duration.ofMinutes(windowMinutes))

        synchronized(requestLog) {
            val deque = requestLog.computeIfAbsent(compositeKey) { ConcurrentLinkedDeque() }

            while (deque.peekFirst()?.isBefore(cutoff) == true) {
                deque.pollFirst()
            }

            if (deque.size >= maxRequests) {
                return false
            }

            deque.addLast(now)
            return true
        }
    }

    fun cleanupExpired(windowMinutes: Long = 15) {
        val cutoff = Instant.now().minus(Duration.ofMinutes(windowMinutes))
        synchronized(requestLog) {
            val iterator = requestLog.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val deque = entry.value
                while (deque.peekFirst()?.isBefore(cutoff) == true) {
                    deque.pollFirst()
                }
                if (deque.isEmpty()) {
                    iterator.remove()
                }
            }
        }
    }

    fun resolveClientIp(request: HttpServletRequest): String {
        return request.remoteAddr?.takeIf { it.isNotBlank() } ?: "unknown"
    }

    fun currentEntriesCount(): Int = requestLog.size

    fun reset() {
        requestLog.clear()
    }
}
