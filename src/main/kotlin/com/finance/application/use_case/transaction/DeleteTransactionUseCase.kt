package com.finance.application.use_case.transaction

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
class DeleteTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    @Transactional
    fun execute(userId: UUID, transactionId: UUID, deleteAllFuture: Boolean = false) {
        val transaction = transactionRepository.findById(transactionId)
            ?: throw TransactionNotFoundException()

        if (transaction.responsibleId != userId) {
            throw UnauthorizedException("You can only delete transactions you created")
        }

        val transactionsToDelete = mutableListOf(transaction)

        if (deleteAllFuture && transaction.recurrenceGroupId != null) {
            val futureTransactions = transactionRepository.findFutureTransactionsByGroupId(
                groupId = transaction.recurrenceGroupId,
                date = transaction.date
            )
            transactionsToDelete.addAll(futureTransactions)
        }

        // Revert balances for COMPLETED transactions
        for (tx in transactionsToDelete) {
            if (tx.status == TransactionStatus.COMPLETED) {
                val account = accountRepository.findById(tx.accountId)
                    ?: throw AccountNotFoundException("Account not found")

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
                accountRepository.save(account)
            }
        }

        transactionRepository.deleteAll(transactionsToDelete.map { it.id })
    }
}
