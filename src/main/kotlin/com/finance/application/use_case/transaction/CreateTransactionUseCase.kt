package com.finance.application.use_case.transaction

import com.finance.application.dto.transaction.AccountSplitRequest
import com.finance.application.dto.transaction.CreateTransactionRequest
import com.finance.application.dto.transaction.TransactionResponse
import com.finance.application.mapper.TransactionDtoMapper
import com.finance.application.service.CreditCardService
import com.finance.domain.entity.AccountEntity
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.enum.RecurrenceFrequency
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.exception.InvalidTransactionException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.repository.UserRepository
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
    private val groupMemberRepository: GroupMemberRepository
) {
    @Transactional
    fun execute(userId: UUID, request: CreateTransactionRequest): List<TransactionResponse> {
        if (request.recurrenceInstallments < 0)
            throw InvalidTransactionException("A quantidade de parcelas não pode ser negativa.")

        if (request.recurrenceInterval <= 0 && request.isRecurring)
            throw InvalidTransactionException("O Intervalo não pode ser nulo ou negativo.")

        val creditCard = request.creditCardId?.let { creditCardService.requireCardAccess(it, userId) }

        val rawSplits = request.splits?.takeIf { it.isNotEmpty() }
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

        val accountsById = mutableMapOf<UUID, AccountEntity>()
        if (creditCard == null) {
            for (split in effectiveSplits) {
                val acc = accountRepository.findById(split.accountId) ?: throw AccountNotFoundException("Conta não encontrada")
                val hasAccess = acc.ownerId == userId || (acc.groupId != null && groupMemberRepository.existsByGroupIdAndUserId(acc.groupId!!, userId))
                if (!hasAccess) throw AccessDeniedException("Você não tem permissão para lançar transações na conta ${acc.name}.")
                accountsById[acc.id] = acc
            }
        }

        var installments = if (request.isRecurring) request.recurrenceInstallments else 1
        val isFixedInstallmentPlan = request.isRecurring && !request.isInfinite && installments > 1

        if (request.isRecurring && request.isInfinite) {
            installments = 1
            installments = when (request.recurrenceFrequency) {
                RecurrenceFrequency.DAILY -> installments * 365
                RecurrenceFrequency.WEEKLY -> installments * 48
                RecurrenceFrequency.MONTHLY -> installments * 12
                RecurrenceFrequency.YEARLY -> installments
                else -> 0
            }
        }

        val divideTotal = isFixedInstallmentPlan && request.installmentAmountMode?.uppercase() == "TOTAL"

        val baseInvoiceYm: YearMonth? = if (creditCard != null) {
            if (request.invoiceMonth != null && request.invoiceYear != null) {
                YearMonth.of(request.invoiceYear, request.invoiceMonth)
            } else {
                creditCard.defaultInvoiceMonthYear(request.date)
            }
        } else null

        val transactionsToSave = mutableListOf<TransactionEntity>()

        for (split in effectiveSplits) {
            val recurrenceGroupId = if (request.isRecurring) UUID.randomUUID() else null
            var currentDate = request.date
            val baseInstallmentAmount = if (divideTotal) split.amount / installments else split.amount
            val remainder = if (divideTotal) split.amount % installments else 0
            val splitAccount = accountsById[split.accountId]
            val itemResponsibleId = if (effectiveSplits.size > 1 && splitAccount != null) splitAccount.ownerId else userId

            for (i in 0 until installments) {
                val itemAmount = if (divideTotal && i == 0) baseInstallmentAmount + remainder else baseInstallmentAmount
                val itemInvoiceYm = if (creditCard != null && baseInvoiceYm != null) {
                    if (i == 0) baseInvoiceYm
                    else {
                        val monthsOffset = when (request.recurrenceFrequency) {
                            RecurrenceFrequency.YEARLY -> (i * request.recurrenceInterval * 12).toLong()
                            RecurrenceFrequency.MONTHLY, null -> (i * request.recurrenceInterval).toLong()
                            else -> creditCard.defaultInvoiceMonthYear(currentDate).let {
                                return@let java.time.temporal.ChronoUnit.MONTHS.between(baseInvoiceYm, it)
                            }
                        }
                        baseInvoiceYm.plusMonths(monthsOffset)
                    }
                } else null

                val itemStatus = if (creditCard != null) TransactionStatus.COMPLETED else if (i == 0) request.status else TransactionStatus.PENDING

                val transaction = TransactionEntity(
                    title = request.title,
                    amount = itemAmount,
                    date = currentDate,
                    responsibleId = itemResponsibleId,
                    accountId = split.accountId,
                    type = request.type,
                    destinationAccountId = if (creditCard != null) null else request.destinationAccountId,
                    categoryId = request.categoryId,
                    status = itemStatus,
                    recurrenceGroupId = recurrenceGroupId,
                    recurrenceFrequency = request.recurrenceFrequency.takeIf { request.isRecurring },
                    recurrenceInterval = request.recurrenceInterval.takeIf { request.isRecurring },
                    isInfinite = request.isInfinite && request.isRecurring,
                    creditCardId = creditCard?.id,
                    invoiceMonth = itemInvoiceYm?.monthValue,
                    invoiceYear = itemInvoiceYm?.year,
                    installmentNumber = if (isFixedInstallmentPlan) i + 1 else null,
                    totalInstallments = if (isFixedInstallmentPlan) installments else null
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
                            val destinationAccount = request.destinationAccountId?.let { accountRepository.findById(it) }
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

        val users = userRepository.findAll().associateBy { it.id }
        val savedTransactions = transactionRepository.saveAll(transactionsToSave)
        return savedTransactions.map { TransactionDtoMapper.toResponse(it, users[it.responsibleId]) }
    }
}
