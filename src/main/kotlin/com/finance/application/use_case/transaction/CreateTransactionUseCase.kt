package com.finance.application.use_case.transaction

import com.finance.application.dto.transaction.CreateTransactionRequest
import com.finance.application.dto.transaction.TransactionResponse
import com.finance.application.mapper.TransactionDtoMapper
import com.finance.application.service.CreditCardService
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
        val groupId = if (request.isRecurring) UUID.randomUUID() else null
        val transactionsToSave = mutableListOf<TransactionEntity>()
        var currentDate = request.date

        if (request.recurrenceInstallments < 0)
            throw InvalidTransactionException("A quantidade de parcelas não pode ser negativa.")

        if (request.recurrenceInterval <= 0 && request.isRecurring)
            throw InvalidTransactionException("O Intervalo não pode ser nulo ou negativo.")

        val creditCard = request.creditCardId?.let { creditCardService.requireCardAccess(it, userId) }
        val effectiveAccountId = creditCard?.accountId ?: request.accountId

        if (creditCard == null) {
            val acc = accountRepository.findById(effectiveAccountId) ?: throw AccountNotFoundException("Conta não encontrada")
            val hasAccess = acc.ownerId == userId || (acc.groupId != null && groupMemberRepository.existsByGroupIdAndUserId(acc.groupId!!, userId))
            if (!hasAccess) throw AccessDeniedException("Você não tem permissão para lançar transações nesta conta.")
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
        val baseInstallmentAmount = if (divideTotal) request.amount / installments else request.amount
        val remainder = if (divideTotal) request.amount % installments else 0

        val baseInvoiceYm: YearMonth? = if (creditCard != null) {
            if (request.invoiceMonth != null && request.invoiceYear != null) {
                YearMonth.of(request.invoiceYear, request.invoiceMonth)
            } else {
                creditCard.defaultInvoiceMonthYear(request.date)
            }
        } else null

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

            val transaction = TransactionEntity(
                title = request.title,
                amount = itemAmount,
                date = currentDate,
                responsibleId = userId,
                accountId = effectiveAccountId,
                type = request.type,
                destinationAccountId = if (creditCard != null) null else request.destinationAccountId,
                categoryId = request.categoryId,
                status = if (creditCard != null) TransactionStatus.COMPLETED else if (i == 0) request.status else TransactionStatus.PENDING,
                recurrenceGroupId = groupId,
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

            if (request.isRecurring && request.recurrenceFrequency != null) {
                currentDate = when (request.recurrenceFrequency) {
                    RecurrenceFrequency.DAILY -> currentDate.plusDays(request.recurrenceInterval.toLong())
                    RecurrenceFrequency.WEEKLY -> currentDate.plusWeeks(request.recurrenceInterval.toLong())
                    RecurrenceFrequency.MONTHLY -> currentDate.plusMonths(request.recurrenceInterval.toLong())
                    RecurrenceFrequency.YEARLY -> currentDate.plusYears(request.recurrenceInterval.toLong())
                }
            }
        }

        val firstTransaction = transactionsToSave.first()
        // Compras no cartão de crédito não afetam o saldo da conta bancária no momento da compra (apenas no pagamento da fatura).
        if (creditCard == null && firstTransaction.status == TransactionStatus.COMPLETED) {
            val account = accountRepository.findById(effectiveAccountId)
                ?: throw AccountNotFoundException("Conta não encontrada")

            when (firstTransaction.type) {
                TransactionType.INCOME -> account.addBalance(firstTransaction.amount)
                TransactionType.EXPENSE -> account.subtractBalance(firstTransaction.amount)
                TransactionType.TRANSFER -> {
                    account.subtractBalance(firstTransaction.amount)
                    val destinationAccount = request.destinationAccountId?.let { accountRepository.findById(it) }
                        ?: throw AccountNotFoundException("Destinatario não encontrado")
                    destinationAccount.addBalance(firstTransaction.amount)
                    accountRepository.save(destinationAccount)
                }
            }
            accountRepository.save(account)
        }

        val responsible = userRepository.findById(userId)
        val savedTransactions = transactionRepository.saveAll(transactionsToSave)
        return savedTransactions.map { TransactionDtoMapper.toResponse(it, responsible) }
    }
}
