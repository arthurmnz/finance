package com.finance.application.use_case.transaction

import com.finance.application.dto.transaction.TransactionResponse
import com.finance.application.mapper.TransactionDtoMapper
import com.finance.application.service.AccountAccessService
import com.finance.application.service.InvoicePaymentTransactionResolver
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.exception.TransactionNotFoundException
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
    private val creditCardRepository: CreditCardRepository,
    private val accountAccessService: AccountAccessService
) {
    @Transactional
    fun execute(userId: UUID, transactionId: UUID): TransactionResponse {
        val transaction = transactionRepository.findById(transactionId)
            ?: throw TransactionNotFoundException()

        accountAccessService.requireTransactionMutationAccess(
            transaction = transaction,
            userId = userId,
            accountRepository = accountRepository,
            creditCardRepository = creditCardRepository,
            unauthorizedMessage = "You can only change status of transactions you created"
        )

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

        val invoice = InvoicePaymentTransactionResolver.findInvoiceForPaymentTx(
            tx = transaction,
            creditCardInvoiceRepository = creditCardInvoiceRepository,
            creditCardRepository = creditCardRepository
        )
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
            newStatus = newStatus,
            newGeneralCategoryId = transaction.generalCategoryId
        )

        val updatedTransaction = transactionRepository.save(transaction)
        val category = updatedTransaction.categoryId?.let { categoryRepository.findById(it) }
        val parentCategory = category?.parentId?.let { categoryRepository.findById(it) }
        val generalCategory = updatedTransaction.generalCategoryId?.let { gid ->
            when {
                gid == category?.id -> category
                gid == parentCategory?.id -> parentCategory
                else -> categoryRepository.findById(gid)
            }
        }
        return TransactionDtoMapper.toResponse(
            updatedTransaction,
            userRepository.findById(transaction.responsibleId),
            category,
            parentCategory,
            generalCategory
        )
    }
}
