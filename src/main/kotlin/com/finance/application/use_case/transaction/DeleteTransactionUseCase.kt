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
    fun execute(userId: UUID, transactionId: UUID) {
        val transaction = transactionRepository.findById(transactionId)
            ?: throw TransactionNotFoundException()

        if (transaction.responsibleId != userId) {
            throw UnauthorizedException("You can only delete transactions you created")
        }

        if (transaction.status == TransactionStatus.COMPLETED) {
            val account = accountRepository.findById(transaction.accountId)
                ?: throw AccountNotFoundException("Account not found")

            when (transaction.type) {
                TransactionType.INCOME -> account.subtractBalance(transaction.amount)
                TransactionType.EXPENSE -> account.addBalance(transaction.amount)
                TransactionType.TRANSFER -> {
                    account.addBalance(transaction.amount)
                    val destinationAccount = transaction.destinationAccountId?.let { accountRepository.findById(it) }
                        ?: throw AccountNotFoundException("Destination account not found")
                    destinationAccount.subtractBalance(transaction.amount)
                    accountRepository.save(destinationAccount)
                }
            }
            accountRepository.save(account)
        }

        transactionRepository.delete(transactionId)
    }
}
