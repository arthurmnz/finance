package com.finance.application.use_case.transaction

import com.finance.application.dto.transaction.TransactionResponse
import com.finance.application.mapper.TransactionDtoMapper
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.exception.TransactionNotFoundException
import com.finance.domain.exception.UnauthorizedException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ToggleTransactionStatusUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    @Transactional
    fun execute(userId: UUID, transactionId: UUID): TransactionResponse {
        val transaction = transactionRepository.findById(transactionId)
            ?: throw TransactionNotFoundException()

        if (transaction.responsibleId != userId) {
            throw UnauthorizedException("You can only change status of transactions you created")
        }

        val account = accountRepository.findById(transaction.accountId)
            ?: throw AccountNotFoundException("Account not found")

        val newStatus = if (transaction.status == TransactionStatus.COMPLETED) {
            TransactionStatus.PENDING
        } else {
            TransactionStatus.COMPLETED
        }

        // Logic to revert or apply balances
        // If moving to COMPLETED -> apply amount
        // If moving to PENDING -> revert amount
        val multiplier = if (newStatus == TransactionStatus.COMPLETED) 1 else -1
        val effectiveAmount = transaction.amount * multiplier

        when (transaction.type) {
            TransactionType.INCOME -> account.addBalance(effectiveAmount)
            TransactionType.EXPENSE -> account.subtractBalance(effectiveAmount)
            TransactionType.TRANSFER -> {
                account.subtractBalance(effectiveAmount)
                val destinationAccount = transaction.destinationAccountId?.let { accountRepository.findById(it) }
                    ?: throw AccountNotFoundException("Destination account not found")
                destinationAccount.addBalance(effectiveAmount)
                accountRepository.save(destinationAccount)
            }
        }
        
        accountRepository.save(account)

        // Update status
        transaction.update(
            newTitle = transaction.title,
            newAmount = transaction.amount,
            newDate = transaction.date,
            newCategoryId = transaction.categoryId,
            newStatus = newStatus
        )

        val updatedTransaction = transactionRepository.save(transaction)
        return TransactionDtoMapper.toResponse(updatedTransaction)
    }
}
