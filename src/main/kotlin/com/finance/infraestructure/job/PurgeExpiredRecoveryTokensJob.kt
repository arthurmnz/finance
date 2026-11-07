package com.finance.infraestructure.job

import com.finance.infraestructure.persistence.repository.SpringDataPasswordRecoveryTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class PurgeExpiredRecoveryTokensJob(
    private val springDataTokenRepository: SpringDataPasswordRecoveryTokenRepository,
) {

    private val logger = LoggerFactory.getLogger(PurgeExpiredRecoveryTokensJob::class.java)

    // Executa diariamente às 03:00 da manhã
    @Scheduled(cron = "\${app.security.token-purge-cron:0 0 3 * * *}")
    @Transactional
    fun purgeExpiredTokens() {
        val cutoff = LocalDateTime.now().minusDays(1)
        val deletedCount = springDataTokenRepository.deleteByExpiresAtBefore(cutoff)
        if (deletedCount > 0) {
            logger.info("Purga automática removeu {} tokens de recuperação de senha expirados.", deletedCount)
        }
    }
}
