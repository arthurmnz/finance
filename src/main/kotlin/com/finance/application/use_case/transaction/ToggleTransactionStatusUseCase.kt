package com.finance.application.use_case.transaction

import com.finance.application.dto.transaction.TransactionResponse
import com.finance.application.mapper.TransactionDtoMapper
import com.finance.domain.entity.CreditCardInvoiceEntity
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.exception.TransactionNotFoundException
import com.finance.domain.exception.UnauthorizedException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.CreditCardInvoiceRepository
import com.finance.domain.repository.CreditCardRepository
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ToggleTransactionStatusUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
    private val creditCardInvoiceRepository: CreditCardInvoiceRepository,
    private val creditCardRepository: CreditCardRepository
) {
    @Transactional
    fun execute(userId: UUID, transactionId: UUID): TransactionResponse {
        val transaction = transactionRepository.findById(transactionId)
            ?: throw TransactionNotFoundException()

        if (transaction.responsibleId != userId) {
            throw UnauthorizedException("You can only change status of transactions you created")
        }

        val newStatus = if (transaction.status == TransactionStatus.COMPLETED) {
            TransactionStatus.PENDING
        } else {
            TransactionStatus.COMPLETED
        }

        if (transaction.creditCardId == null) {
            val account = accountRepository.findById(transaction.accountId)
                ?: throw AccountNotFoundException("Account not found")

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
        }

        val invoice = findInvoiceForPaymentTx(transaction)
        if (invoice != null) {
            if (newStatus == TransactionStatus.COMPLETED) {
                invoice.addPayment(transaction.amount)
            } else {
                invoice.subtractPayment(transaction.amount)
            }
            creditCardInvoiceRepository.save(invoice)
        }

        transaction.update(
            newTitle = transaction.title,
            newAmount = transaction.amount,
            newDate = transaction.date,
            newCategoryId = transaction.categoryId,
            newStatus = newStatus
        )

        val updatedTransaction = transactionRepository.save(transaction)
        val category = updatedTransaction.categoryId?.let { categoryRepository.findById(it) }
        return TransactionDtoMapper.toResponse(
            updatedTransaction,
            userRepository.findById(transaction.responsibleId),
            category
        )
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
