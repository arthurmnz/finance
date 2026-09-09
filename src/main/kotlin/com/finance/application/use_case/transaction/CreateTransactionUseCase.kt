package com.finance.application.use_case.transaction

import com.finance.application.dto.transaction.CreateTransactionRequest
import com.finance.application.dto.transaction.TransactionResponse
import com.finance.application.mapper.TransactionDtoMapper
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.enum.RecurrenceFrequency
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.repository.AccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CreateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    @Transactional
    fun execute(userId: UUID, request: CreateTransactionRequest): List<TransactionResponse> {
        val groupId = if (request.isRecurring) UUID.randomUUID() else null
        val transactionsToSave = mutableListOf<TransactionEntity>()
        var currentDate = request.date

        var installments = if (request.isRecurring or (request.recurrenceInstallments < 0)) request.recurrenceInstallments else 1

        if (request.isRecurring and request.isInfinite) {
            installments = 1
            installments = when (request.recurrenceFrequency) {
                RecurrenceFrequency.DAILY -> installments * 364;
                RecurrenceFrequency.WEEKLY -> installments * 47;
                RecurrenceFrequency.MONTHLY -> installments * 11;
                RecurrenceFrequency.YEARLY -> installments
                else -> 0
            }
        }

        for (i in 0 until installments) {
            val transaction = TransactionEntity(
                title = request.title,
                amount = request.amount,
                date = currentDate,
                responsibleId = userId,
                accountId = request.accountId,
                type = request.type,
                destinationAccountId = request.destinationAccountId,
                categoryId = request.categoryId,
                status = if (i == 0) request.status else TransactionStatus.PENDING,
                recurrenceGroupId = groupId,
                recurrenceFrequency = request.recurrenceFrequency.takeIf { request.isRecurring },
                recurrenceInterval = request.recurrenceInterval.takeIf { request.isRecurring },
                isInfinite = request.isInfinite && request.isRecurring
            )
            transactionsToSave.add(transaction)

            // Calculate next date
            if (request.isRecurring && request.recurrenceFrequency != null) {
                currentDate = when (request.recurrenceFrequency) {
                    RecurrenceFrequency.DAILY -> currentDate.plusDays(request.recurrenceInterval.toLong())
                    RecurrenceFrequency.WEEKLY -> currentDate.plusWeeks(request.recurrenceInterval.toLong())
                    RecurrenceFrequency.MONTHLY -> currentDate.plusMonths(request.recurrenceInterval.toLong())
                    RecurrenceFrequency.YEARLY -> currentDate.plusYears(request.recurrenceInterval.toLong())
                }
            }
        }

        // Balance logic only for the first transaction if it is COMPLETED
        val firstTransaction = transactionsToSave.first()
        if (firstTransaction.status == TransactionStatus.COMPLETED) {
            val account = accountRepository.findById(request.accountId) 
                ?: throw AccountNotFoundException("Account not found")

            when (firstTransaction.type) {
                TransactionType.INCOME -> account.addBalance(firstTransaction.amount)
                TransactionType.EXPENSE -> account.subtractBalance(firstTransaction.amount)
                TransactionType.TRANSFER -> {
                    account.subtractBalance(firstTransaction.amount)
                    val destinationAccount = request.destinationAccountId?.let { accountRepository.findById(it) }
                        ?: throw AccountNotFoundException("Destination account not found")
                    destinationAccount.addBalance(firstTransaction.amount)
                    accountRepository.save(destinationAccount)
                }
            }
            accountRepository.save(account)
        }

        val savedTransactions = transactionRepository.saveAll(transactionsToSave)
        return savedTransactions.map { TransactionDtoMapper.toResponse(it) }
    }
}
