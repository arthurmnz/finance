package com.finance.application.job

import com.finance.domain.entity.TransactionEntity
import com.finance.domain.enum.RecurrenceFrequency
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.repository.CreditCardRepository
import com.finance.domain.repository.TransactionRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class RecurrenceExpanderJob(
    private val transactionRepository: TransactionRepository,
    private val creditCardRepository: CreditCardRepository
) {
    @Scheduled(cron = "0 0 3 * * *")
    fun expandInfiniteRecurrences() {
        val latestTransactions = transactionRepository.findLatestInfiniteTransactions()

        val transactionsToSave = mutableListOf<TransactionEntity>()
        val limitDate = LocalDate.now().plusMonths(12)

        for (latest in latestTransactions) {
            val frequency = latest.recurrenceFrequency ?: continue
            val interval = latest.recurrenceInterval ?: 1
            val card = latest.creditCardId?.let { creditCardRepository.findById(it) }

            var currentDate = calculateNextDate(latest.date, frequency, interval)

            while (currentDate.isBefore(limitDate)) {
                val ym = card?.defaultInvoiceMonthYear(currentDate)
                val newTransaction = TransactionEntity(
                    title = latest.title,
                    amount = latest.amount,
                    date = currentDate,
                    responsibleId = latest.responsibleId,
                    accountId = latest.accountId,
                    type = latest.type,
                    destinationAccountId = latest.destinationAccountId,
                    categoryId = latest.categoryId,
                    status = if (card != null) TransactionStatus.COMPLETED else TransactionStatus.PENDING,
                    recurrenceGroupId = latest.recurrenceGroupId,
                    recurrenceFrequency = latest.recurrenceFrequency,
                    recurrenceInterval = latest.recurrenceInterval,
                    isInfinite = true,
                    creditCardId = latest.creditCardId,
                    invoiceMonth = ym?.monthValue,
                    invoiceYear = ym?.year
                )
                transactionsToSave.add(newTransaction)
                currentDate = calculateNextDate(currentDate, frequency, interval)
            }
        }

        if (transactionsToSave.isNotEmpty()) {
            transactionRepository.saveAll(transactionsToSave)
        }
    }

    private fun calculateNextDate(currentDate: LocalDate, frequency: RecurrenceFrequency, interval: Int): LocalDate {
        return when (frequency) {
            RecurrenceFrequency.DAILY -> currentDate.plusDays(interval.toLong())
            RecurrenceFrequency.WEEKLY -> currentDate.plusWeeks(interval.toLong())
            RecurrenceFrequency.MONTHLY -> currentDate.plusMonths(interval.toLong())
            RecurrenceFrequency.YEARLY -> currentDate.plusYears(interval.toLong())
        }
    }
}
