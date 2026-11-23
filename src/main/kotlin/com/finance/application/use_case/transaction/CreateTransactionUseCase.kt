package com.finance.application.use_case.transaction

import com.finance.application.dto.transaction.AccountSplitRequest
import com.finance.application.dto.transaction.CreateTransactionRequest
import com.finance.application.dto.transaction.TransactionResponse
import com.finance.application.mapper.TransactionDtoMapper
import com.finance.application.service.AccountAccessService
import com.finance.application.service.CreditCardService
import com.finance.application.service.TransactionCategoryValidator
import com.finance.application.service.TransactionInputValidator
import com.finance.domain.entity.AccountEntity
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.enum.RecurrenceFrequency
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.exception.InvalidTransactionException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.repository.UserRepository
import com.finance.domain.service.InstallmentAmountAllocator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth
import java.util.UUID

@Service
class CreateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val creditCardService: CreditCardService,
    private val userRepository: UserRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val categoryRepository: CategoryRepository,
    private val accountAccessService: AccountAccessService
) {
    @Transactional
    fun execute(userId: UUID, request: CreateTransactionRequest): List<TransactionResponse> {
        if (request.amount <= 0) {
            throw InvalidTransactionException("O valor da transação deve ser maior que zero.")
        }

        val normalizedMode = (request.installmentAmountMode ?: "INSTALLMENT").uppercase()
        if (normalizedMode !in VALID_INSTALLMENT_AMOUNT_MODES) {
            throw InvalidTransactionException(
                "Modo de valor de parcela inválido. Valores permitidos: TOTAL, INSTALLMENT, CUSTOM."
            )
        }

        TransactionInputValidator.validateRecurrenceLimits(
            recurrenceInstallments = request.recurrenceInstallments,
            recurrenceInterval = request.recurrenceInterval
        )
        TransactionInputValidator.validateInvoicePeriod(
            invoiceMonth = request.invoiceMonth,
            invoiceYear = request.invoiceYear
        )

        var installments = if (request.isRecurring) request.recurrenceInstallments else 1
        val isFixedInstallmentPlan = request.isRecurring && !request.isInfinite && installments > 1

        if (request.isRecurring && request.isInfinite) {
            installments = when (request.recurrenceFrequency) {
                RecurrenceFrequency.DAILY -> 365
                RecurrenceFrequency.WEEKLY -> 48
                RecurrenceFrequency.MONTHLY -> 12
                RecurrenceFrequency.YEARLY -> 1
                else -> 0
            }
        }

        val rawSplits = request.splits?.takeIf { it.isNotEmpty() }

        val customAmounts: List<Int>? = if (request.installmentAmounts != null || normalizedMode == "CUSTOM") {
            if (!isFixedInstallmentPlan) {
                throw InvalidTransactionException(
                    "Parcelas personalizadas exigem um parcelamento fixo (isRecurring = true, isInfinite = false e mais de 1 parcela)."
                )
            }
            if (rawSplits != null) {
                throw InvalidTransactionException(
                    "Parcelas personalizadas não podem ser usadas simultaneamente com rateio entre múltiplas contas."
                )
            }
            val amounts = request.installmentAmounts
                ?: throw InvalidTransactionException(
                    "A lista de valores das parcelas (installmentAmounts) é obrigatória no modo CUSTOM."
                )
            if (amounts.size != installments) {
                throw InvalidTransactionException(
                    "A quantidade de valores informados (${amounts.size}) deve ser igual ao número de parcelas ($installments)."
                )
            }
            if (amounts.any { it <= 0 }) {
                throw InvalidTransactionException("Todos os valores das parcelas devem ser maiores que zero.")
            }
            val sum = amounts.sumOf { it.toLong() }
            if (sum > Int.MAX_VALUE || sum != request.amount.toLong()) {
                throw InvalidTransactionException(
                    "A soma das parcelas ($sum) deve ser igual ao valor total informado (${request.amount})."
                )
            }
            amounts
        } else {
            if (normalizedMode == "INSTALLMENT" && request.amount.toLong() * installments > Int.MAX_VALUE) {
                throw InvalidTransactionException("O valor total do parcelamento excede o limite máximo permitido.")
            }
            if (isFixedInstallmentPlan && normalizedMode == "TOTAL" && rawSplits == null) {
                // Valida antecipadamente se o valor total permite ao menos 1 centavo por parcela
                InstallmentAmountAllocator.allocate(request.amount, installments, request.remainderStrategy)
            }
            null
        }

        val resolvedCategories = TransactionCategoryValidator.validateSelection(
            categoryRepository = categoryRepository,
            groupMemberRepository = groupMemberRepository,
            userId = userId,
            transactionType = request.type,
            categoryId = request.categoryId,
            generalCategoryId = request.generalCategoryId,
            specificCategoryId = request.specificCategoryId
        )

        val creditCard = request.creditCardId?.let { creditCardService.requireCardAccess(it, userId) }

        if (request.generatesDebt && (creditCard != null || request.type != TransactionType.TRANSFER)) {
            throw InvalidTransactionException("Apenas transferências bancárias podem gerar saldo devedor.")
        }

        if (rawSplits != null) {
            if (creditCard != null) {
                throw InvalidTransactionException("Compras no cartão de crédito não suportam divisão entre múltiplas contas bancárias.")
            }
            if (request.type == TransactionType.TRANSFER) {
                throw InvalidTransactionException("Transferências não suportam divisão entre múltiplas contas.")
            }
            if (rawSplits.size > 10) {
                throw InvalidTransactionException("A transação pode ser dividida em no máximo 10 contas distintas.")
            }
            if (rawSplits.map { it.accountId }.distinct().size != rawSplits.size) {
                throw InvalidTransactionException("Não é permitido repetir a mesma conta na divisão.")
            }
            if (rawSplits.any { it.amount <= 0 }) {
                throw InvalidTransactionException("O valor de cada conta na divisão deve ser maior que zero.")
            }
        }

        val effectiveSplits: List<AccountSplitRequest> = rawSplits ?: listOf(
            AccountSplitRequest(
                accountId = creditCard?.accountId ?: request.accountId,
                amount = request.amount
            )
        )

        val divideTotal = customAmounts == null && isFixedInstallmentPlan && normalizedMode == "TOTAL"
        val splitInstallmentAmountsList: List<List<Int>> = effectiveSplits.map { split ->
            when {
                customAmounts != null -> customAmounts
                divideTotal -> InstallmentAmountAllocator.allocate(split.amount, installments, request.remainderStrategy)
                else -> {
                    if (normalizedMode == "INSTALLMENT" && split.amount.toLong() * installments > Int.MAX_VALUE) {
                        throw InvalidTransactionException("O valor total do parcelamento excede o limite máximo permitido.")
                    }
                    List(installments) { split.amount }
                }
            }
        }

        val accountsById = mutableMapOf<UUID, AccountEntity>()
        if (creditCard == null) {
            for (split in effectiveSplits) {
                val acc = accountRepository.findById(split.accountId) ?: throw AccountNotFoundException("Conta não encontrada")
                if (request.type == TransactionType.TRANSFER) {
                    accountAccessService.requireTransferSourceAccess(acc, userId)
                } else {
                    accountAccessService.requireAccess(
                        acc,
                        userId,
                        "Você não tem permissão para registrar transações na conta informada."
                    )
                }
                accountsById[acc.id] = acc
            }
        }

        var preloadedDestAccount: AccountEntity? = null
        if (creditCard == null && request.type == TransactionType.TRANSFER) {
            val destId = request.destinationAccountId
                ?: throw InvalidTransactionException("Conta de destino é obrigatória para transferências.")
            if (destId == request.accountId) {
                throw InvalidTransactionException("A conta de destino deve ser diferente da conta de origem.")
            }
            val destAccount = accountRepository.findById(destId)
                ?: throw AccountNotFoundException("Destinatario não encontrado")
            accountAccessService.requireAccess(
                destAccount,
                userId,
                "Você não tem permissão para transferir para a conta de destino informada."
            )

            if (request.generatesDebt) {
                val sourceAccount = accountsById[request.accountId]
                    ?: accountRepository.findById(request.accountId)
                    ?: throw AccountNotFoundException("Conta não encontrada")
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
            preloadedDestAccount = destAccount
        }

        if (resolvedCategories.allCategories.any { it.groupId != null }) {
            val involvedGroupIds = buildList {
                if (creditCard != null) {
                    add(creditCard.groupId)
                } else {
                    accountsById.values.forEach { add(it.groupId) }
                    if (request.type == TransactionType.TRANSFER) {
                        add(preloadedDestAccount?.groupId)
                    }
                }
            }
            TransactionCategoryValidator.validateGroupScope(resolvedCategories, involvedGroupIds)
        }

        val baseInvoiceYm: YearMonth? = if (creditCard != null) {
            if (request.invoiceMonth != null && request.invoiceYear != null) {
                YearMonth.of(request.invoiceYear, request.invoiceMonth)
            } else {
                creditCard.defaultInvoiceMonthYear(request.date)
            }
        } else null

        val transactionsToSave = mutableListOf<TransactionEntity>()

        for ((splitIndex, split) in effectiveSplits.withIndex()) {
            val recurrenceGroupId = if (request.isRecurring) UUID.randomUUID() else null
            var currentDate = request.date
            val splitAmounts = splitInstallmentAmountsList[splitIndex]
            val splitAccount = accountsById[split.accountId]
            val itemResponsibleId = if (effectiveSplits.size > 1 && splitAccount != null) (splitAccount.ownerId ?: userId) else userId

            for (i in 0 until installments) {
                val itemAmount = splitAmounts[i]
                val itemInvoiceYm = if (creditCard != null && baseInvoiceYm != null) {
                    creditCard.installmentInvoiceMonthYear(
                        baseInvoiceYm = baseInvoiceYm,
                        offset = i,
                        frequency = request.recurrenceFrequency,
                        interval = request.recurrenceInterval,
                        currentDate = currentDate
                    )
                } else null

                val itemDate = if (creditCard != null && isFixedInstallmentPlan && i > 0 && baseInvoiceYm != null && itemInvoiceYm != null) {
                    creditCard.installmentItemDate(
                        baseInvoiceYm = baseInvoiceYm,
                        itemInvoiceYm = itemInvoiceYm,
                        frequency = request.recurrenceFrequency,
                        fallbackDate = currentDate
                    )
                } else {
                    currentDate
                }

                val itemStatus = if (creditCard != null) TransactionStatus.COMPLETED else if (i == 0) request.status else TransactionStatus.PENDING

                val transaction = TransactionEntity(
                    title = request.title,
                    amount = itemAmount,
                    date = itemDate,
                    responsibleId = itemResponsibleId,
                    accountId = split.accountId,
                    type = request.type,
                    destinationAccountId = if (creditCard != null) null else request.destinationAccountId,
                    categoryId = resolvedCategories.effectiveCategoryId,
                    status = itemStatus,
                    recurrenceGroupId = recurrenceGroupId,
                    recurrenceFrequency = request.recurrenceFrequency.takeIf { request.isRecurring },
                    recurrenceInterval = request.recurrenceInterval.takeIf { request.isRecurring },
                    isInfinite = request.isInfinite && request.isRecurring,
                    creditCardId = creditCard?.id,
                    invoiceMonth = itemInvoiceYm?.monthValue,
                    invoiceYear = itemInvoiceYm?.year,
                    installmentNumber = if (isFixedInstallmentPlan) i + 1 else null,
                    totalInstallments = if (isFixedInstallmentPlan) installments else null,
                    generatesDebt = request.generatesDebt && request.type == TransactionType.TRANSFER && creditCard == null,
                    generalCategoryId = resolvedCategories.effectiveGeneralCategoryId
                )
                transactionsToSave.add(transaction)

                if (creditCard == null && i == 0 && itemStatus == TransactionStatus.COMPLETED) {
                    val account = splitAccount ?: accountRepository.findById(split.accountId)
                        ?: throw AccountNotFoundException("Conta não encontrada")

                    when (transaction.type) {
                        TransactionType.INCOME -> account.addBalance(transaction.amount)
                        TransactionType.EXPENSE -> account.subtractBalance(transaction.amount)
                        TransactionType.TRANSFER -> {
                            account.subtractBalance(transaction.amount)
                            val destinationAccount = preloadedDestAccount
                                ?: request.destinationAccountId?.let { accountRepository.findById(it) }
                                ?: throw AccountNotFoundException("Destinatario não encontrado")
                            destinationAccount.addBalance(transaction.amount)
                            accountRepository.save(destinationAccount)
                        }
                    }
                    accountRepository.save(account)
                }

                if (request.isRecurring && request.recurrenceFrequency != null) {
                    currentDate = when (request.recurrenceFrequency) {
                        RecurrenceFrequency.DAILY -> currentDate.plusDays(request.recurrenceInterval.toLong())
                        RecurrenceFrequency.WEEKLY -> currentDate.plusWeeks(request.recurrenceInterval.toLong())
                        RecurrenceFrequency.MONTHLY -> currentDate.plusMonths(request.recurrenceInterval.toLong())
                        RecurrenceFrequency.YEARLY -> currentDate.plusYears(request.recurrenceInterval.toLong())
                    }
                }
            }
        }

        val responseCategory = resolvedCategories.specificCategory ?: resolvedCategories.generalCategory
        val responseParent = if (resolvedCategories.specificCategory != null) resolvedCategories.generalCategory else null
        val users = userRepository.findAll().associateBy { it.id }
        val savedTransactions = transactionRepository.saveAll(transactionsToSave)
        return savedTransactions.map {
            TransactionDtoMapper.toResponse(
                entity = it,
                responsibleUser = users[it.responsibleId],
                category = responseCategory,
                parentCategory = responseParent,
                generalCategoryEntity = resolvedCategories.generalCategory
            )
        }
    }

    companion object {
        private val VALID_INSTALLMENT_AMOUNT_MODES = setOf("TOTAL", "INSTALLMENT", "CUSTOM")
    }
}
