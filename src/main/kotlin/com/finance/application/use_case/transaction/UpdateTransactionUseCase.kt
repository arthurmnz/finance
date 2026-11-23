package com.finance.application.use_case.transaction

import com.finance.application.dto.transaction.TransactionResponse
import com.finance.application.dto.transaction.UpdateTransactionRequest
import com.finance.application.mapper.TransactionDtoMapper
import com.finance.application.service.AccountAccessService
import com.finance.application.service.InvoicePaymentTransactionResolver
import com.finance.application.service.ResolvedTransactionCategories
import com.finance.application.service.TransactionCategoryValidator
import com.finance.application.service.TransactionInputValidator
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.enum.CategoryLevel
import com.finance.domain.enum.RecurrenceFrequency
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.exception.CreditCardNotFoundException
import com.finance.domain.exception.InvalidTransactionException
import com.finance.domain.exception.TransactionNotFoundException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.CreditCardInvoiceRepository
import com.finance.domain.repository.CreditCardRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.repository.UserRepository
import com.finance.domain.service.InstallmentAmountAllocator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Service
class UpdateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val creditCardInvoiceRepository: CreditCardInvoiceRepository,
    private val creditCardRepository: CreditCardRepository,
    private val accountAccessService: AccountAccessService
) {
    @Transactional
    fun execute(userId: UUID, transactionId: UUID, request: UpdateTransactionRequest): TransactionResponse {
        if (request.amount <= 0) {
            throw InvalidTransactionException("O valor da transação deve ser maior que zero.")
        }

        TransactionInputValidator.validateRecurrenceLimits(
            recurrenceInstallments = request.recurrenceInstallments,
            recurrenceInterval = request.recurrenceInterval
        )
        TransactionInputValidator.validateInvoicePeriod(
            invoiceMonth = request.invoiceMonth,
            invoiceYear = request.invoiceYear
        )

        val normalizedMode = request.installmentAmountMode?.uppercase()
        if (normalizedMode != null && normalizedMode !in VALID_UPDATE_INSTALLMENT_MODES) {
            throw InvalidTransactionException(
                "Modo de valor de parcela inválido. Valores permitidos: TOTAL, INSTALLMENT."
            )
        }

        val transaction = transactionRepository.findById(transactionId)
            ?: throw TransactionNotFoundException()

        accountAccessService.requireTransactionMutationAccess(
            transaction = transaction,
            userId = userId,
            accountRepository = accountRepository,
            creditCardRepository = creditCardRepository,
            unauthorizedMessage = "You can only edit transactions you created"
        )

        val effectiveGeneratesDebt = request.generatesDebt ?: transaction.generatesDebt
        if (effectiveGeneratesDebt) {
            if (transaction.type != TransactionType.TRANSFER || transaction.creditCardId != null) {
                throw InvalidTransactionException("Apenas transferências bancárias podem gerar saldo devedor.")
            }
            val sourceAccount = accountRepository.findById(transaction.accountId)
                ?: throw AccountNotFoundException("Conta não encontrada")
            val destId = transaction.destinationAccountId
                ?: throw InvalidTransactionException("Conta de destino é obrigatória para transferências.")
            val destAccount = accountRepository.findById(destId)
                ?: throw AccountNotFoundException("Destinatario não encontrado")
            if (sourceAccount.isJoint || destAccount.isJoint) {
                throw InvalidTransactionException("Transferências envolvendo conta conjunta não podem gerar saldo devedor.")
            }
            val sourceOwnerId = sourceAccount.ownerId
                ?: throw InvalidTransactionException("Transferências envolvendo conta conjunta não podem gerar saldo devedor.")
            val destOwnerId = destAccount.ownerId
                ?: throw InvalidTransactionException("Transferências envolvendo conta conjunta não podem gerar saldo devedor.")
            if (sourceOwnerId != userId) {
                throw AccessDeniedException("Apenas o titular da conta de origem pode registrar a transferência como dívida.")
            }
            if (sourceOwnerId == destOwnerId) {
                throw InvalidTransactionException("A transferência deve ser entre pessoas distintas para gerar saldo devedor.")
            }
            val sharedGroupId = destAccount.groupId ?: sourceAccount.groupId
            val bothInSharedGroup = sharedGroupId != null &&
                groupMemberRepository.existsByGroupIdAndUserId(sharedGroupId, sourceOwnerId) &&
                groupMemberRepository.existsByGroupIdAndUserId(sharedGroupId, destOwnerId)
            if (!bothInSharedGroup) {
                throw InvalidTransactionException("As contas da transferência devem estar vinculadas a um grupo em comum entre os dois usuários para gerar saldo devedor.")
            }
        }

        val categoryUnchanged = isCategoryUnchanged(transaction, request)
        val resolvedCategories = if (categoryUnchanged) {
            loadExistingCategories(transaction)
        } else {
            TransactionCategoryValidator.validateSelection(
                categoryRepository = categoryRepository,
                groupMemberRepository = groupMemberRepository,
                userId = userId,
                transactionType = transaction.type,
                categoryId = request.categoryId,
                generalCategoryId = request.generalCategoryId,
                specificCategoryId = request.specificCategoryId
            ).also { resolved ->
                if (resolved.allCategories.any { it.groupId != null }) {
                    val involvedGroupIds = buildList {
                        val cardId = transaction.creditCardId
                        if (cardId != null) {
                            val card = creditCardRepository.findById(cardId) ?: throw CreditCardNotFoundException()
                            add(card.groupId)
                        } else {
                            add(accountRepository.findById(transaction.accountId)?.groupId)
                            if (transaction.type == TransactionType.TRANSFER) {
                                add(transaction.destinationAccountId?.let { accountRepository.findById(it)?.groupId })
                            }
                        }
                    }
                    TransactionCategoryValidator.validateGroupScope(resolved, involvedGroupIds)
                }
            }
        }

        val transactionsToUpdate = mutableListOf(transaction)
        var shouldRegenerateFutures = false
        var regeneratedAllocatedAmounts: List<Int>? = null
        var inPlaceAllocatedAmounts: List<Int>? = null

        if (normalizedMode == "TOTAL" && (!request.updateAllFuture || transaction.recurrenceGroupId == null)) {
            throw InvalidTransactionException(
                "O modo TOTAL na edição só é suportado para parcelamentos finitos com updateAllFuture = true."
            )
        }

        if (request.updateAllFuture && transaction.recurrenceGroupId != null) {
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
                    unauthorizedMessage = "You can only edit transactions you created"
                )
            }

            val recurrenceChanged = (request.isRecurring != null && request.isRecurring == false) ||
                    (request.recurrenceFrequency != null && request.recurrenceFrequency != transaction.recurrenceFrequency) ||
                    (request.recurrenceInterval != null && request.recurrenceInterval != transaction.recurrenceInterval) ||
                    (request.isInfinite != null && request.isInfinite != transaction.isInfinite) ||
                    (request.recurrenceInstallments != null)

            if (recurrenceChanged) {
                val willRegenerate = request.isRecurring != false
                if (willRegenerate && request.isRecurring == true) {
                    val regInterval = request.recurrenceInterval ?: transaction.recurrenceInterval ?: 1
                    val regIsInf = request.isInfinite ?: transaction.isInfinite
                    val regLimit = if (regIsInf) 12 else (request.recurrenceInstallments ?: 12)
                    TransactionInputValidator.validateRecurrenceLimits(regLimit, regInterval)
                    if (normalizedMode == "TOTAL") {
                        if (regIsInf) {
                            throw InvalidTransactionException("O modo TOTAL não é suportado para recorrências infinitas.")
                        }
                        regeneratedAllocatedAmounts = InstallmentAmountAllocator.allocate(
                            totalCents = request.amount,
                            count = 1 + regLimit,
                            strategy = request.remainderStrategy
                        )
                    } else if (!regIsInf && request.amount.toLong() * (1 + regLimit) > Int.MAX_VALUE) {
                        throw InvalidTransactionException("O valor total do parcelamento excede o limite máximo permitido.")
                    }
                } else if (normalizedMode == "TOTAL") {
                    throw InvalidTransactionException("O modo TOTAL na edição requer recorrência ativa.")
                }

                for (futureTx in futureTransactions) {
                    if (futureTx.creditCardId == null && futureTx.status == TransactionStatus.COMPLETED) {
                        val acc = accountRepository.findById(futureTx.accountId) ?: continue
                        when (futureTx.type) {
                            TransactionType.INCOME -> acc.subtractBalance(futureTx.amount)
                            TransactionType.EXPENSE -> acc.addBalance(futureTx.amount)
                            TransactionType.TRANSFER -> {
                                acc.addBalance(futureTx.amount)
                                futureTx.destinationAccountId?.let { accountRepository.findById(it) }?.let { dest ->
                                    dest.subtractBalance(futureTx.amount)
                                    accountRepository.save(dest)
                                }
                            }
                        }
                        accountRepository.save(acc)
                    }
                }
                transactionRepository.deleteAll(futureTransactions.map { it.id })
                shouldRegenerateFutures = willRegenerate
            } else {
                val sortedFutures = futureTransactions.sortedWith(
                    compareBy<TransactionEntity, Int?>(nullsLast()) { it.installmentNumber }
                        .thenBy { it.date }
                )
                transactionsToUpdate.addAll(sortedFutures)
                if (normalizedMode == "TOTAL") {
                    if (transaction.isInfinite) {
                        throw InvalidTransactionException("O modo TOTAL não é suportado para recorrências infinitas.")
                    }
                    if (transaction.installmentNumber != null && transaction.installmentNumber > 1) {
                        throw InvalidTransactionException(
                            "Para redistribuir o valor total, edite a partir da 1ª parcela ou utilize a edição em lote do grupo."
                        )
                    }
                    inPlaceAllocatedAmounts = InstallmentAmountAllocator.allocate(
                        totalCents = request.amount,
                        count = transactionsToUpdate.size,
                        strategy = request.remainderStrategy
                    )
                } else if (transactionsToUpdate.size > 1) {
                    val willPropagateToFutures = request.updateFutureAmounts == true ||
                        (request.updateFutureAmounts == null &&
                            (sortedFutures.any { it.isInfinite } || request.amount != transaction.amount))
                    if (willPropagateToFutures) {
                        if (request.amount.toLong() * transactionsToUpdate.size > Int.MAX_VALUE) {
                            throw InvalidTransactionException("O valor total do parcelamento excede o limite máximo permitido.")
                        }
                    } else if (!transaction.isInfinite) {
                        val projectedTotal = request.amount.toLong() + sortedFutures.sumOf { it.amount.toLong() }
                        if (projectedTotal > Int.MAX_VALUE) {
                            throw InvalidTransactionException("O valor total do parcelamento excede o limite máximo permitido.")
                        }
                    }
                }
            }
        }

        val originalAmount = transaction.amount

        for ((index, tx) in transactionsToUpdate.withIndex()) {
            val targetAmount = when {
                regeneratedAllocatedAmounts != null && tx.id == transactionId -> regeneratedAllocatedAmounts[0]
                inPlaceAllocatedAmounts != null -> inPlaceAllocatedAmounts[index]
                tx.id == transactionId -> request.amount
                request.updateFutureAmounts == true -> request.amount
                request.updateFutureAmounts == null && (tx.isInfinite || request.amount != originalAmount) -> request.amount
                else -> tx.amount
            }

            if (tx.creditCardId == null) {
                val account = accountRepository.findById(tx.accountId)
                    ?: throw AccountNotFoundException("Account not found")

                val targetStatus = if (tx.id == transactionId) request.status else tx.status
                val targetDate = if (tx.id == transactionId) request.date else tx.date
                val targetGeneratesDebt = request.generatesDebt ?: tx.generatesDebt
                val oldStatus = tx.status
                val oldAmount = tx.amount
                val invoice = InvoicePaymentTransactionResolver.findInvoiceForPaymentTx(
                    tx = tx,
                    creditCardInvoiceRepository = creditCardInvoiceRepository,
                    creditCardRepository = creditCardRepository
                )

                val oldEffectiveAmount = if (oldStatus == TransactionStatus.COMPLETED) oldAmount else 0
                val newEffectiveAmount = if (targetStatus == TransactionStatus.COMPLETED) targetAmount else 0
                val delta = newEffectiveAmount - oldEffectiveAmount

                if (delta != 0) {
                    when (tx.type) {
                        TransactionType.INCOME -> {
                            if (delta > 0) account.addBalance(delta) else account.subtractBalance(-delta)
                        }
                        TransactionType.EXPENSE -> {
                            if (delta > 0) account.subtractBalance(delta) else account.addBalance(-delta)
                        }
                        TransactionType.TRANSFER -> {
                            val destinationAccount = tx.destinationAccountId?.let { accountRepository.findById(it) }
                                ?: throw AccountNotFoundException("Destination account not found")
                            if (delta > 0) {
                                account.subtractBalance(delta)
                                destinationAccount.addBalance(delta)
                            } else {
                                account.addBalance(-delta)
                                destinationAccount.subtractBalance(-delta)
                            }
                            accountRepository.save(destinationAccount)
                        }
                    }
                }

                val effectiveCatId = if (categoryUnchanged) transaction.categoryId else resolvedCategories.effectiveCategoryId
                val effectiveGenCatId = if (categoryUnchanged) transaction.generalCategoryId else resolvedCategories.effectiveGeneralCategoryId

                tx.update(
                    newTitle = request.title,
                    newAmount = targetAmount,
                    newDate = targetDate,
                    newCategoryId = effectiveCatId,
                    newStatus = targetStatus,
                    newGeneratesDebt = targetGeneratesDebt,
                    newGeneralCategoryId = effectiveGenCatId
                )

                if (invoice != null) {
                    if (newEffectiveAmount != oldEffectiveAmount) {
                        val updatedPaid = (invoice.paidAmount.toLong() + delta.toLong())
                            .coerceIn(0L, Int.MAX_VALUE.toLong())
                            .toInt()
                        invoice.setPaidAmount(updatedPaid)
                        creditCardInvoiceRepository.save(invoice)
                    }
                }

                accountRepository.save(account)
            } else {
                val targetDate = if (tx.id == transactionId) request.date else tx.date
                val newMonth = if (tx.id == transactionId && request.invoiceMonth != null) request.invoiceMonth else tx.invoiceMonth
                val newYear = if (tx.id == transactionId && request.invoiceYear != null) request.invoiceYear else tx.invoiceYear
                val effectiveCatId = if (categoryUnchanged) transaction.categoryId else resolvedCategories.effectiveCategoryId
                val effectiveGenCatId = if (categoryUnchanged) transaction.generalCategoryId else resolvedCategories.effectiveGeneralCategoryId

                tx.update(
                    newTitle = request.title,
                    newAmount = targetAmount,
                    newDate = targetDate,
                    newCategoryId = effectiveCatId,
                    newStatus = tx.status,
                    newInvoiceMonth = newMonth,
                    newInvoiceYear = newYear,
                    newGeneralCategoryId = effectiveGenCatId
                )
            }
        }

        val responsible = userRepository.findById(transaction.responsibleId)
        val responseCategory = resolvedCategories.specificCategory ?: resolvedCategories.generalCategory
        val responseParent = if (resolvedCategories.specificCategory != null) resolvedCategories.generalCategory else null
        val savedTransactions = transactionRepository.saveAll(transactionsToUpdate)
        val responseList = savedTransactions.map {
            TransactionDtoMapper.toResponse(
                entity = it,
                responsibleUser = responsible,
                category = responseCategory,
                parentCategory = responseParent,
                generalCategoryEntity = resolvedCategories.generalCategory
            )
        }.toMutableList()

        if (shouldRegenerateFutures && request.isRecurring == true) {
            var currentDate = request.date
            val freq = request.recurrenceFrequency ?: transaction.recurrenceFrequency ?: RecurrenceFrequency.MONTHLY
            val interval = request.recurrenceInterval ?: transaction.recurrenceInterval ?: 1
            val isInf = request.isInfinite ?: transaction.isInfinite
            val limit = if (isInf) 12 else (request.recurrenceInstallments ?: 12)
            TransactionInputValidator.validateRecurrenceLimits(limit, interval)
            val card = transaction.creditCardId?.let {
                creditCardRepository.findById(it) ?: throw CreditCardNotFoundException()
            }
            val baseInvoiceYm = if (!isInf && card != null) {
                when {
                    request.invoiceMonth != null && request.invoiceYear != null ->
                        YearMonth.of(request.invoiceYear, request.invoiceMonth)
                    transaction.invoiceMonth != null && transaction.invoiceYear != null -> {
                        TransactionInputValidator.validateInvoicePeriod(
                            transaction.invoiceMonth,
                            transaction.invoiceYear
                        )
                        YearMonth.of(transaction.invoiceYear!!, transaction.invoiceMonth!!)
                    }
                    else ->
                        card.defaultInvoiceMonthYear(request.date)
                }
            } else null
            val baseInstallmentNumber = transaction.installmentNumber ?: 1
            val totalInstallments = if (!isInf && card != null) baseInstallmentNumber + limit else null

            val newlyGenerated = mutableListOf<TransactionEntity>()
            for (i in 1..limit) {
                currentDate = calculateNextDate(currentDate, freq, interval)
                val itemInvoiceYm = when {
                    !isInf && card != null && baseInvoiceYm != null -> card.installmentInvoiceMonthYear(
                        baseInvoiceYm = baseInvoiceYm,
                        offset = i,
                        frequency = freq,
                        interval = interval,
                        currentDate = currentDate
                    )
                    isInf && card != null -> card.defaultInvoiceMonthYear(currentDate)
                    else -> null
                }
                val itemDate = if (!isInf && card != null && baseInvoiceYm != null && itemInvoiceYm != null) {
                    card.installmentItemDate(
                        baseInvoiceYm = baseInvoiceYm,
                        itemInvoiceYm = itemInvoiceYm,
                        frequency = freq,
                        fallbackDate = currentDate
                    )
                } else {
                    currentDate
                }
                val newTx = TransactionEntity(
                    title = request.title,
                    amount = regeneratedAllocatedAmounts?.get(i) ?: request.amount,
                    date = itemDate,
                    responsibleId = transaction.responsibleId,
                    accountId = transaction.accountId,
                    type = transaction.type,
                    destinationAccountId = transaction.destinationAccountId,
                    categoryId = if (categoryUnchanged) transaction.categoryId else resolvedCategories.effectiveCategoryId,
                    status = if (transaction.creditCardId != null) TransactionStatus.COMPLETED else TransactionStatus.PENDING,
                    recurrenceGroupId = transaction.recurrenceGroupId,
                    recurrenceFrequency = freq,
                    recurrenceInterval = interval,
                    isInfinite = isInf,
                    creditCardId = transaction.creditCardId,
                    invoiceMonth = itemInvoiceYm?.monthValue,
                    invoiceYear = itemInvoiceYm?.year,
                    installmentNumber = if (!isInf && card != null) baseInstallmentNumber + i else null,
                    totalInstallments = totalInstallments,
                    generatesDebt = request.generatesDebt ?: transaction.generatesDebt,
                    generalCategoryId = if (categoryUnchanged) transaction.generalCategoryId else resolvedCategories.effectiveGeneralCategoryId
                )
                newlyGenerated.add(newTx)
            }
            if (newlyGenerated.isNotEmpty()) {
                val savedNewFutures = transactionRepository.saveAll(newlyGenerated)
                responseList.addAll(savedNewFutures.map {
                    TransactionDtoMapper.toResponse(
                        entity = it,
                        responsibleUser = responsible,
                        category = responseCategory,
                        parentCategory = responseParent,
                        generalCategoryEntity = resolvedCategories.generalCategory
                    )
                })
            }
        }

        return responseList.first { it.id == transactionId }
    }

    private fun isCategoryUnchanged(tx: TransactionEntity, req: UpdateTransactionRequest): Boolean {
        if (req.specificCategoryId != null && req.categoryId != null && req.specificCategoryId != req.categoryId) {
            return false
        }
        val reqLeafId = req.specificCategoryId ?: req.categoryId ?: req.generalCategoryId
        if (tx.categoryId == null && tx.generalCategoryId == null) {
            return reqLeafId == null
        }
        if (tx.generalCategoryId != null && tx.categoryId != tx.generalCategoryId) {
            return reqLeafId == tx.categoryId &&
                (req.generalCategoryId == null || req.generalCategoryId == tx.generalCategoryId) &&
                (req.specificCategoryId != null || req.categoryId != null)
        }
        if (tx.generalCategoryId == null) {
            if (reqLeafId != tx.categoryId) return false
            if (req.generalCategoryId == null) return true
            val existingCat = tx.categoryId?.let { categoryRepository.findById(it) }
            return existingCat?.parentId == req.generalCategoryId
        }
        return reqLeafId == tx.categoryId &&
            req.specificCategoryId == null &&
            (req.generalCategoryId == null || req.generalCategoryId == tx.categoryId)
    }

    private fun loadExistingCategories(tx: TransactionEntity): ResolvedTransactionCategories {
        if (tx.categoryId == null && tx.generalCategoryId == null) {
            return ResolvedTransactionCategories()
        }
        val cat = tx.categoryId?.let { categoryRepository.findById(it) }
        val gen = tx.generalCategoryId?.let { gid ->
            if (gid == cat?.id) cat else categoryRepository.findById(gid)
        } ?: cat?.parentId?.let { categoryRepository.findById(it) }
        return if (cat?.level == CategoryLevel.SPECIFIC) {
            ResolvedTransactionCategories(generalCategory = gen, specificCategory = cat)
        } else {
            ResolvedTransactionCategories(generalCategory = cat ?: gen, specificCategory = null)
        }
    }

    private fun calculateNextDate(currentDate: LocalDate, frequency: RecurrenceFrequency, interval: Int): LocalDate {
        return when (frequency) {
            RecurrenceFrequency.DAILY -> currentDate.plusDays(interval.toLong())
            RecurrenceFrequency.WEEKLY -> currentDate.plusWeeks(interval.toLong())
            RecurrenceFrequency.MONTHLY -> currentDate.plusMonths(interval.toLong())
            RecurrenceFrequency.YEARLY -> currentDate.plusYears(interval.toLong())
        }
    }

    companion object {
        private val VALID_UPDATE_INSTALLMENT_MODES = setOf("TOTAL", "INSTALLMENT")
    }
}
