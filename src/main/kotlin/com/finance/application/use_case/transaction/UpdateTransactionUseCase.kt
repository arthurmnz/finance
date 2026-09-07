package com.finance.application.use_case.transaction

import com.finance.application.dto.transaction.UpdateTransactionRequest
import com.finance.application.dto.transaction.TransactionResponse
import com.finance.application.mapper.TransactionDtoMapper
import com.finance.domain.exception.TransactionNotFoundException
import com.finance.domain.exception.UnauthorizedException
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.repository.AccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UpdateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    @Transactional
    fun execute(userId: UUID, transactionId: UUID, request: UpdateTransactionRequest): TransactionResponse {
        val transaction = transactionRepository.findById(transactionId)
            ?: throw TransactionNotFoundException()

        if (transaction.responsibleId != userId) {
            throw UnauthorizedException("You can only edit transactions you created (temporarily logic)")
        }

        val oldStatus = transaction.status
        val oldAmount = transaction.amount
        val newStatus = request.status
        val newAmount = request.amount

        // Helper func
        fun adjustBalancesForTx(tx: com.finance.domain.entity.TransactionEntity, amountToAdjust: Int, revert: Boolean = false) {
            val account = accountRepository.findById(tx.accountId)
                ?: throw AccountNotFoundException("Account not found")

            val multiplier = if (revert) -1 else 1
            val effectiveAmount = amountToAdjust * multiplier

            when (tx.type) {
                TransactionType.INCOME -> account.addBalance(effectiveAmount)
                TransactionType.EXPENSE -> account.subtractBalance(effectiveAmount)
                TransactionType.TRANSFER -> {
                    account.subtractBalance(effectiveAmount)
                    val destinationAccount = tx.destinationAccountId?.let { accountRepository.findById(it) }
                        ?: throw AccountNotFoundException("Destination account not found")
                    destinationAccount.addBalance(effectiveAmount)
                    accountRepository.save(destinationAccount)
                }
            }
            accountRepository.save(account)
        }

        val transactionsToUpdate = mutableListOf(transaction)
        var shouldRegenerateFutures = false

        if (request.updateAllFuture && transaction.recurrenceGroupId != null) {
            val futureTransactions = transactionRepository.findFutureTransactionsByGroupId(transaction.recurrenceGroupId, transaction.date)
            
            // Check if recurrence rule changed
            val ruleChanged = (request.isRecurring != null && request.isRecurring != (transaction.recurrenceFrequency != null)) ||
                              (request.recurrenceFrequency != null && request.recurrenceFrequency != transaction.recurrenceFrequency) ||
                              (request.recurrenceInterval != null && request.recurrenceInterval != transaction.recurrenceInterval) ||
                              (request.isInfinite != null && request.isInfinite != transaction.isInfinite)
            
            if (ruleChanged) {
                shouldRegenerateFutures = true
                // Revert balances for COMPLETED future txs and delete them
                for (tx in futureTransactions) {
                    if (tx.status == TransactionStatus.COMPLETED) {
                        adjustBalancesForTx(tx, tx.amount, revert = true)
                    }
                }
                transactionRepository.deleteAll(futureTransactions.map { it.id })
            } else {
                transactionsToUpdate.addAll(futureTransactions)
            }
        }

        for (tx in transactionsToUpdate) {
            val isCurrent = tx.id == transaction.id
            val txOldStatus = tx.status
            val txOldAmount = tx.amount
            
            val txNewStatus = if (isCurrent) request.status else txOldStatus // Only current tx gets status updated normally, futures stay as is unless requested? No, usually futures stay pending.
            
            if (txOldStatus == TransactionStatus.COMPLETED && txNewStatus == TransactionStatus.COMPLETED) {
                val diff = request.amount - txOldAmount
                if (diff != 0) {
                    adjustBalancesForTx(tx, diff)
                }
            } else if (txOldStatus == TransactionStatus.PENDING && txNewStatus == TransactionStatus.COMPLETED) {
                adjustBalancesForTx(tx, request.amount)
            } else if (txOldStatus == TransactionStatus.COMPLETED && txNewStatus == TransactionStatus.PENDING) {
                adjustBalancesForTx(tx, txOldAmount, revert = true)
            }

            tx.update(
                newTitle = request.title,
                newAmount = request.amount,
                newDate = if (isCurrent) request.date else tx.date, // Don't change dates of futures here
                newCategoryId = request.categoryId ?: tx.categoryId,
                newStatus = txNewStatus
            )
        }

        // If rule changed, we must regenerate the future ones here
        val savedTransactions = transactionRepository.saveAll(transactionsToUpdate)
        val responseList = savedTransactions.map { TransactionDtoMapper.toResponse(it) }.toMutableList()

        if (shouldRegenerateFutures && request.isRecurring == true) {
            var currentDate = request.date
            val freq = request.recurrenceFrequency ?: transaction.recurrenceFrequency ?: com.finance.domain.enum.RecurrenceFrequency.MONTHLY
            val interval = request.recurrenceInterval ?: transaction.recurrenceInterval ?: 1
            val isInf = request.isInfinite ?: transaction.isInfinite
            
            val limit = if (isInf) 12 else (request.recurrenceInstallments ?: 12)
            
            val newlyGenerated = mutableListOf<com.finance.domain.entity.TransactionEntity>()
            for (i in 1..limit) { // Start from 1 because 0 is the current transaction
                currentDate = calculateNextDate(currentDate, freq, interval)
                
                val newTx = com.finance.domain.entity.TransactionEntity(
                    title = request.title,
                    amount = request.amount,
                    date = currentDate,
                    responsibleId = userId,
                    accountId = transaction.accountId,
                    type = transaction.type,
                    destinationAccountId = transaction.destinationAccountId,
                    categoryId = request.categoryId,
                    status = TransactionStatus.PENDING,
                    recurrenceGroupId = transaction.recurrenceGroupId,
                    recurrenceFrequency = freq,
                    recurrenceInterval = interval,
                    isInfinite = isInf
                )
                newlyGenerated.add(newTx)
            }
            if (newlyGenerated.isNotEmpty()) {
                val savedNewFutures = transactionRepository.saveAll(newlyGenerated)
                responseList.addAll(savedNewFutures.map { TransactionDtoMapper.toResponse(it) })
            }
        }

        return responseList.first { it.id == transactionId }
    }

    private fun calculateNextDate(
        currentDate: java.time.LocalDateTime,
        frequency: com.finance.domain.enum.RecurrenceFrequency,
        interval: Int
    ): java.time.LocalDateTime {
        return when (frequency) {
            com.finance.domain.enum.RecurrenceFrequency.DAILY -> currentDate.plusDays(interval.toLong())
            com.finance.domain.enum.RecurrenceFrequency.WEEKLY -> currentDate.plusWeeks(interval.toLong())
            com.finance.domain.enum.RecurrenceFrequency.MONTHLY -> currentDate.plusMonths(interval.toLong())
            com.finance.domain.enum.RecurrenceFrequency.YEARLY -> currentDate.plusYears(interval.toLong())
        }
    }

}
