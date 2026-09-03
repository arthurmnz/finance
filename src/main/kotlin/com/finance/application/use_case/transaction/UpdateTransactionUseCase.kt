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
        fun adjustBalances(amountToAdjust: Int, revert: Boolean = false) {
            val account = accountRepository.findById(transaction.accountId)
                ?: throw AccountNotFoundException("Account not found")

            val multiplier = if (revert) -1 else 1
            val effectiveAmount = amountToAdjust * multiplier

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
        }

        if (oldStatus == TransactionStatus.COMPLETED && newStatus == TransactionStatus.COMPLETED) {
            val diff = newAmount - oldAmount
            if (diff != 0) {
                adjustBalances(diff)
            }
        } else if (oldStatus == TransactionStatus.PENDING && newStatus == TransactionStatus.COMPLETED) {
            adjustBalances(newAmount)
        } else if (oldStatus == TransactionStatus.COMPLETED && newStatus == TransactionStatus.PENDING) {
            adjustBalances(oldAmount, revert = true)
        }

        transaction.update(
            newTitle = request.title,
            newAmount = request.amount,
            newDate = request.date,
            newCategoryId = request.categoryId,
            newStatus = request.status
        )

        val updatedTransaction = transactionRepository.save(transaction)
        return TransactionDtoMapper.toResponse(updatedTransaction)
    }
}
