package com.finance.application.use_case.transaction

import com.finance.application.dto.transaction.TransactionResponse
import com.finance.application.dto.transaction.UpdateInstallmentGroupAmountsRequest
import com.finance.application.mapper.CategoryBatchLoader
import com.finance.application.mapper.TransactionDtoMapper
import com.finance.application.service.AccountAccessService
import com.finance.application.service.InvoicePaymentTransactionResolver
import com.finance.domain.entity.AccountEntity
import com.finance.domain.entity.CreditCardInvoiceEntity
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.exception.InvalidTransactionException
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
class UpdateInstallmentGroupAmountsUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val creditCardRepository: CreditCardRepository,
    private val creditCardInvoiceRepository: CreditCardInvoiceRepository,
    private val accountAccessService: AccountAccessService,
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository
) {
    @Transactional
    fun execute(
        userId: UUID,
        recurrenceGroupId: UUID,
        request: UpdateInstallmentGroupAmountsRequest
    ): List<TransactionResponse> {
        if (request.items.isEmpty()) {
            throw InvalidTransactionException("A lista de parcelas para atualização não pode ser vazia.")
        }
        if (request.items.map { it.transactionId }.distinct().size != request.items.size) {
            throw InvalidTransactionException("Não é permitido informar IDs de transações duplicados.")
        }
        if (request.items.any { it.amount <= 0 }) {
            throw InvalidTransactionException("O valor de cada parcela deve ser maior que zero.")
        }

        val groupTransactions = transactionRepository
            .findByRecurrenceGroupIdOrderByInstallmentNumberAscDateAsc(recurrenceGroupId)
        if (groupTransactions.isEmpty()) {
            throw TransactionNotFoundException("Grupo de parcelamento não encontrado.")
        }

        for (tx in groupTransactions) {
            accountAccessService.requireTransactionMutationAccess(
                transaction = tx,
                userId = userId,
                accountRepository = accountRepository,
                creditCardRepository = creditCardRepository,
                unauthorizedMessage = "You can only edit transactions you created"
            )
        }

        val groupTxById = groupTransactions.associateBy { it.id }
        for (item in request.items) {
            if (!groupTxById.containsKey(item.transactionId)) {
                throw InvalidTransactionException(
                    "A transação ${item.transactionId} não pertence ao grupo de parcelamento informado."
                )
            }
        }

        val itemAmountById = request.items.associate { it.transactionId to it.amount }
        val projectedTotal = groupTransactions.sumOf { (itemAmountById[it.id] ?: it.amount).toLong() }
        if (projectedTotal > Int.MAX_VALUE) {
            throw InvalidTransactionException("O valor total do grupo de parcelamento excede o limite máximo permitido.")
        }

        val targetTransactions = groupTransactions
            .filter { itemAmountById.containsKey(it.id) }
            .sortedWith(
                compareBy<TransactionEntity, Int?>(nullsLast()) { it.installmentNumber }
                    .thenBy { it.date }
            )

        val accountsToSave = mutableMapOf<UUID, AccountEntity>()
        val invoicesToSave = mutableMapOf<UUID, CreditCardInvoiceEntity>()
        for (tx in targetTransactions) {
            val newAmount = itemAmountById.getValue(tx.id)
            val oldAmount = tx.amount
            val delta = newAmount - oldAmount

            if (tx.creditCardId == null && tx.status == TransactionStatus.COMPLETED && delta != 0) {
                val sourceAccount = accountsToSave.getOrPut(tx.accountId) {
                    accountRepository.findById(tx.accountId)
                        ?: throw AccountNotFoundException("Account not found")
                }
                when (tx.type) {
                    TransactionType.INCOME -> {
                        if (delta > 0) sourceAccount.addBalance(delta) else sourceAccount.subtractBalance(-delta)
                    }
                    TransactionType.EXPENSE -> {
                        if (delta > 0) sourceAccount.subtractBalance(delta) else sourceAccount.addBalance(-delta)
                    }
                    TransactionType.TRANSFER -> {
                        val destId = tx.destinationAccountId
                            ?: throw AccountNotFoundException("Destination account not found")
                        val destAccount = accountsToSave.getOrPut(destId) {
                            accountRepository.findById(destId)
                                ?: throw AccountNotFoundException("Destination account not found")
                        }
                        if (delta > 0) {
                            sourceAccount.subtractBalance(delta)
                            destAccount.addBalance(delta)
                        } else {
                            sourceAccount.addBalance(-delta)
                            destAccount.subtractBalance(-delta)
                        }
                    }
                }

                val resolvedInvoice = InvoicePaymentTransactionResolver.findInvoiceForPaymentTx(
                    tx = tx,
                    creditCardInvoiceRepository = creditCardInvoiceRepository,
                    creditCardRepository = creditCardRepository
                )
                if (resolvedInvoice != null) {
                    val invoice = invoicesToSave.getOrPut(resolvedInvoice.id) { resolvedInvoice }
                    val updatedPaid = (invoice.paidAmount.toLong() + delta.toLong())
                        .coerceIn(0L, Int.MAX_VALUE.toLong())
                        .toInt()
                    invoice.setPaidAmount(updatedPaid)
                }
            }
        }

        for (tx in targetTransactions) {
            val newAmount = itemAmountById.getValue(tx.id)
            tx.update(
                newTitle = tx.title,
                newAmount = newAmount,
                newDate = tx.date,
                newCategoryId = tx.categoryId,
                newStatus = tx.status,
                newInvoiceMonth = tx.invoiceMonth,
                newInvoiceYear = tx.invoiceYear,
                newGeneratesDebt = tx.generatesDebt,
                newGeneralCategoryId = tx.generalCategoryId
            )
        }

        accountsToSave.values.forEach { accountRepository.save(it) }
        invoicesToSave.values.forEach { creditCardInvoiceRepository.save(it) }
        val savedTransactions = transactionRepository.saveAll(targetTransactions)
            .sortedWith(
                compareBy<TransactionEntity, Int?>(nullsLast()) { it.installmentNumber }
                    .thenBy { it.date }
            )

        val users = userRepository.findAll().associateBy { it.id }
        val categoryMap = CategoryBatchLoader.loadForTransactions(categoryRepository, savedTransactions)

        return savedTransactions.map { tx ->
            val cat = tx.categoryId?.let { categoryMap[it] }
            val parent = cat?.parentId?.let { categoryMap[it] }
            val generalCat = tx.generalCategoryId?.let { categoryMap[it] }
            TransactionDtoMapper.toResponse(tx, users[tx.responsibleId], cat, parent, generalCat)
        }
    }
}
