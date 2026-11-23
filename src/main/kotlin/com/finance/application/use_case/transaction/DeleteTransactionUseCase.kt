package com.finance.application.use_case.transaction

import com.finance.application.service.AccountAccessService
import com.finance.application.service.InvoicePaymentTransactionResolver
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.exception.TransactionNotFoundException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.CreditCardInvoiceRepository
import com.finance.domain.repository.CreditCardRepository
import com.finance.domain.repository.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DeleteTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val creditCardInvoiceRepository: CreditCardInvoiceRepository,
    private val creditCardRepository: CreditCardRepository,
    private val accountAccessService: AccountAccessService
) {
    @Transactional
    fun execute(userId: UUID, transactionId: UUID, deleteAllFuture: Boolean = false) {
        val transaction = transactionRepository.findById(transactionId)
            ?: throw TransactionNotFoundException()

        accountAccessService.requireTransactionMutationAccess(
            transaction = transaction,
            userId = userId,
            accountRepository = accountRepository,
            creditCardRepository = creditCardRepository,
            unauthorizedMessage = "You can only delete transactions you created"
        )

        val transactionsToDelete = mutableListOf(transaction)

        if (deleteAllFuture && transaction.recurrenceGroupId != null) {
            val futureTransactions = if (transaction.installmentNumber != null) {
                transactionRepository.findFutureTransactionsInGroup(
                    groupId = transaction.recurrenceGroupId,
                    date = transaction.date,
                    installmentNumber = transaction.installmentNumber
                )
            } else {
                transactionRepository.findFutureTransactionsByGroupId(
                    groupId = transaction.recurrenceGroupId,
                    date = transaction.date
                )
            }
            for (futureTx in futureTransactions) {
                accountAccessService.requireTransactionMutationAccess(
                    transaction = futureTx,
                    userId = userId,
                    accountRepository = accountRepository,
                    creditCardRepository = creditCardRepository,
                    unauthorizedMessage = "You can only delete transactions you created"
                )
            }
            transactionsToDelete.addAll(futureTransactions)
        }

        for (tx in transactionsToDelete) {
            if (tx.creditCardId == null && tx.status == TransactionStatus.COMPLETED) {
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

            if (tx.status == TransactionStatus.COMPLETED) {
                val invoice = InvoicePaymentTransactionResolver.findInvoiceForPaymentTx(
                    tx = tx,
                    creditCardInvoiceRepository = creditCardInvoiceRepository,
                    creditCardRepository = creditCardRepository
                )
                if (invoice != null) {
                    invoice.subtractPayment(tx.amount)
                    creditCardInvoiceRepository.save(invoice)
                }
            }
        }

        transactionRepository.deleteAll(transactionsToDelete.map { it.id })
    }
}
