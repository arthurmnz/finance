package com.finance.application.job

import com.finance.domain.entity.TransactionEntity
import com.finance.domain.enum.RecurrenceFrequency
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.repository.TransactionRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID

@Component
class RecurrenceExpanderJob(
    private val transactionRepository: TransactionRepository
) {
    // Run every day at 3 AM
    @Scheduled(cron = "0 0 3 * * *")
    fun expandInfiniteRecurrences() {
        val latestTransactions = transactionRepository.findLatestInfiniteTransactions()
        
        val transactionsToSave = mutableListOf<TransactionEntity>()
        val limitDate = LocalDateTime.now().plusMonths(12) // Keep a 12-month window

        for (latest in latestTransactions) {
            val frequency = latest.recurrenceFrequency ?: continue
            val interval = latest.recurrenceInterval ?: 1

            var currentDate = calculateNextDate(latest.date, frequency, interval)

            // Generate new transactions until we reach the 12-month limit
            while (currentDate.isBefore(limitDate)) {
                val newTransaction = TransactionEntity(
                    title = latest.title,
                    amount = latest.amount,
                    date = currentDate,
                    responsibleId = latest.responsibleId,
                    accountId = latest.accountId,
                    type = latest.type,
                    destinationAccountId = latest.destinationAccountId,
                    categoryId = latest.categoryId,
                    status = TransactionStatus.PENDING,
                    recurrenceGroupId = latest.recurrenceGroupId,
                    recurrenceFrequency = frequency,
                    recurrenceInterval = interval,
                    isInfinite = true
                )
                transactionsToSave.add(newTransaction)

                currentDate = calculateNextDate(currentDate, frequency, interval)
            }
        }

        if (transactionsToSave.isNotEmpty()) {
            transactionRepository.saveAll(transactionsToSave)
        }
    }

    private fun calculateNextDate(
        currentDate: LocalDateTime,
        frequency: RecurrenceFrequency,
        interval: Int
    ): LocalDateTime {
        return when (frequency) {
            RecurrenceFrequency.DAILY -> currentDate.plusDays(interval.toLong())
            RecurrenceFrequency.WEEKLY -> currentDate.plusWeeks(interval.toLong())
            RecurrenceFrequency.MONTHLY -> currentDate.plusMonths(interval.toLong())
            RecurrenceFrequency.YEARLY -> currentDate.plusYears(interval.toLong())
        }
    }
}
