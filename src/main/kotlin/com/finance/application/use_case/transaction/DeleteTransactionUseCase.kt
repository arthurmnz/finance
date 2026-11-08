package com.finance.application.use_case.transaction

import com.finance.domain.entity.CreditCardInvoiceEntity
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.exception.TransactionNotFoundException
import com.finance.domain.exception.UnauthorizedException
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
    private val creditCardRepository: CreditCardRepository
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
                val invoice = findInvoiceForPaymentTx(tx)
                if (invoice != null) {
                    invoice.subtractPayment(tx.amount)
                    creditCardInvoiceRepository.save(invoice)
                }
            }
        }

        transactionRepository.deleteAll(transactionsToDelete.map { it.id })
    }

    private fun findInvoiceForPaymentTx(tx: TransactionEntity): CreditCardInvoiceEntity? {
        if (tx.creditCardInvoiceId != null) {
            return creditCardInvoiceRepository.findById(tx.creditCardInvoiceId)
        }
        val regex = Regex("""^Pagamento fatura (.+?) \((\d{1,2})/(\d{4})\)$""")
        val match = regex.find(tx.title) ?: return null
        val (cardName, monthStr, yearStr) = match.destructured
        val month = monthStr.toIntOrNull() ?: return null
        val year = yearStr.toIntOrNull() ?: return null

        val cards = creditCardRepository.findAllByOwnerId(tx.responsibleId)
        val card = cards.find { it.name.equals(cardName.trim(), ignoreCase = true) } ?: return null
        return creditCardInvoiceRepository.findByCardAndPeriod(card.id, month, year)
    }
}
