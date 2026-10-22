package com.finance.application.use_case.transaction

import com.finance.application.dto.transaction.TransactionResponse
import com.finance.application.dto.transaction.UpdateTransactionRequest
import com.finance.application.mapper.TransactionDtoMapper
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.enum.RecurrenceFrequency
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.exception.TransactionNotFoundException
import com.finance.domain.exception.UnauthorizedException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class UpdateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val userRepository: UserRepository
) {
    @Transactional
    fun execute(userId: UUID, transactionId: UUID, request: UpdateTransactionRequest): TransactionResponse {
        val transaction = transactionRepository.findById(transactionId)
            ?: throw TransactionNotFoundException()

        if (transaction.responsibleId != userId) {
            throw UnauthorizedException("You can only edit transactions you created")
        }

        val transactionsToUpdate = mutableListOf(transaction)
        var shouldRegenerateFutures = false

        if (request.updateAllFuture && transaction.recurrenceGroupId != null) {
            val futureTransactions = transactionRepository.findFutureTransactionsByGroupId(
                groupId = transaction.recurrenceGroupId,
                date = transaction.date
            )

            val recurrenceChanged = (request.isRecurring != null && request.isRecurring == false) ||
                    (request.recurrenceFrequency != null && request.recurrenceFrequency != transaction.recurrenceFrequency) ||
                    (request.recurrenceInterval != null && request.recurrenceInterval != transaction.recurrenceInterval) ||
                    (request.isInfinite != null && request.isInfinite != transaction.isInfinite) ||
                    (request.recurrenceInstallments != null)

            if (recurrenceChanged) {
                for (futureTx in futureTransactions) {
                    if (futureTx.creditCardId == null && futureTx.status == TransactionStatus.COMPLETED) {
                        val acc = accountRepository.findById(futureTx.accountId) ?: continue
                        when (futureTx.type) {
                            TransactionType.INCOME -> acc.subtractBalance(futureTx.amount)
                            TransactionType.EXPENSE -> acc.addBalance(futureTx.amount)
                            TransactionType.TRANSFER -> {
                                acc.addBalance(futureTx.amount)
                                futureTx.destinationAccountId?.let { accountRepository.findById(it) }?.let { dest ->
                                    dest.subtractBalance(futureTx.amount)
                                    accountRepository.save(dest)
                                }
                            }
                        }
                        accountRepository.save(acc)
                    }
                }
                transactionRepository.deleteAll(futureTransactions.map { it.id })
                shouldRegenerateFutures = request.isRecurring != false
            } else {
                transactionsToUpdate.addAll(futureTransactions)
            }
        }

        for (tx in transactionsToUpdate) {
            if (tx.creditCardId == null) {
                val account = accountRepository.findById(tx.accountId)
                    ?: throw AccountNotFoundException("Account not found")

                if (tx.status == TransactionStatus.COMPLETED) {
                    when (tx.type) {
                        TransactionType.INCOME -> account.subtractBalance(tx.amount)
                        TransactionType.EXPENSE -> account.addBalance(tx.amount)
                        TransactionType.TRANSFER -> {
                            account.addBalance(tx.amount)
                            val destinationAccount = tx.destinationAccountId?.let { accountRepository.findById(it) }
                                ?: throw AccountNotFoundException("Destination account not found")
                            destinationAccount.subtractBalance(tx.amount)
                            accountRepository.save(destinationAccount)
                        }
                    }
                }

                val targetStatus = if (tx.id == transactionId) request.status else tx.status
                val targetDate = if (tx.id == transactionId) request.date else tx.date

                tx.update(
                    newTitle = request.title,
                    newAmount = request.amount,
                    newDate = targetDate,
                    newCategoryId = request.categoryId,
                    newStatus = targetStatus
                )

                if (targetStatus == TransactionStatus.COMPLETED) {
                    when (tx.type) {
                        TransactionType.INCOME -> account.addBalance(request.amount)
                        TransactionType.EXPENSE -> account.subtractBalance(request.amount)
                        TransactionType.TRANSFER -> {
                            account.subtractBalance(request.amount)
                            val destinationAccount = tx.destinationAccountId?.let { accountRepository.findById(it) }
                                ?: throw AccountNotFoundException("Destination account not found")
                            destinationAccount.addBalance(request.amount)
                            accountRepository.save(destinationAccount)
                        }
                    }
                }
                accountRepository.save(account)
            } else {
                val targetDate = if (tx.id == transactionId) request.date else tx.date
                val newMonth = if (tx.id == transactionId && request.invoiceMonth != null) request.invoiceMonth else tx.invoiceMonth
                val newYear = if (tx.id == transactionId && request.invoiceYear != null) request.invoiceYear else tx.invoiceYear
                tx.update(
                    newTitle = request.title,
                    newAmount = request.amount,
                    newDate = targetDate,
                    newCategoryId = request.categoryId,
                    newStatus = tx.status,
                    newInvoiceMonth = newMonth,
                    newInvoiceYear = newYear
                )
            }
        }

        val responsible = userRepository.findById(userId)
        val savedTransactions = transactionRepository.saveAll(transactionsToUpdate)
        val responseList = savedTransactions.map { TransactionDtoMapper.toResponse(it, responsible) }.toMutableList()

        if (shouldRegenerateFutures && request.isRecurring == true) {
            var currentDate = request.date
            val freq = request.recurrenceFrequency ?: transaction.recurrenceFrequency ?: RecurrenceFrequency.MONTHLY
            val interval = request.recurrenceInterval ?: transaction.recurrenceInterval ?: 1
            val isInf = request.isInfinite ?: transaction.isInfinite
            val limit = if (isInf) 12 else (request.recurrenceInstallments ?: 12)

            val newlyGenerated = mutableListOf<TransactionEntity>()
            for (i in 1..limit) {
                currentDate = calculateNextDate(currentDate, freq, interval)
                val newTx = TransactionEntity(
                    title = request.title,
                    amount = request.amount,
                    date = currentDate,
                    responsibleId = userId,
                    accountId = transaction.accountId,
                    type = transaction.type,
                    destinationAccountId = transaction.destinationAccountId,
                    categoryId = request.categoryId,
                    status = if (transaction.creditCardId != null) TransactionStatus.COMPLETED else TransactionStatus.PENDING,
                    recurrenceGroupId = transaction.recurrenceGroupId,
                    recurrenceFrequency = freq,
                    recurrenceInterval = interval,
                    isInfinite = isInf,
                    creditCardId = transaction.creditCardId
                )
                newlyGenerated.add(newTx)
            }
            if (newlyGenerated.isNotEmpty()) {
                val savedNewFutures = transactionRepository.saveAll(newlyGenerated)
                responseList.addAll(savedNewFutures.map { TransactionDtoMapper.toResponse(it, responsible) })
            }
        }

        return responseList.first { it.id == transactionId }
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
