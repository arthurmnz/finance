package com.finance.application.dto.transaction

import com.finance.domain.enum.InstallmentRemainderStrategy
import com.finance.domain.enum.RecurrenceFrequency
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import java.time.LocalDate
import java.util.UUID

data class AccountSplitRequest(
    val accountId: UUID,
    val amount: Int
)

data class CreateTransactionRequest(
    val title: String,
    val amount: Int,
    val date: LocalDate,
    val accountId: UUID,
    val type: TransactionType,
    val destinationAccountId: UUID? = null,
    val categoryId: UUID? = null,
    val generalCategoryId: UUID? = null,
    val specificCategoryId: UUID? = null,
    val status: TransactionStatus = TransactionStatus.PENDING,
    val isRecurring: Boolean = false,
    val recurrenceFrequency: RecurrenceFrequency? = null,
    val recurrenceInterval: Int = 1,
    val recurrenceInstallments: Int = 12,
    val isInfinite: Boolean = false,
    val creditCardId: UUID? = null,
    val invoiceMonth: Int? = null,
    val invoiceYear: Int? = null,
    /**
     * Para compras parceladas / recorrências finitas:
     * - "INSTALLMENT" (padrão): [amount] já é o valor de cada parcela.
     * - "TOTAL": [amount] é o valor total e será dividido pelas parcelas conforme [remainderStrategy].
     * - "CUSTOM": [installmentAmounts] informa o valor exato em centavos de cada parcela.
     */
    val installmentAmountMode: String? = "INSTALLMENT",
    val remainderStrategy: InstallmentRemainderStrategy = InstallmentRemainderStrategy.FIRST_INSTALLMENT,
    val installmentAmounts: List<Int>? = null,
    /**
     * Lista opcional de rateio entre múltiplas contas (máximo de 10 contas distintas).
     * Permitido apenas para EXPENSE e INCOME em contas bancárias.
     */
    val splits: List<AccountSplitRequest>? = null,
    val generatesDebt: Boolean = false
)

data class UpdateTransactionRequest(
    val title: String,
    val amount: Int,
    val date: LocalDate,
    val categoryId: UUID? = null,
    val generalCategoryId: UUID? = null,
    val specificCategoryId: UUID? = null,
    val status: TransactionStatus,
    val updateAllFuture: Boolean = false,
    val updateFutureAmounts: Boolean? = null,
    val isRecurring: Boolean? = null,
    val recurrenceFrequency: RecurrenceFrequency? = null,
    val recurrenceInterval: Int? = null,
    val recurrenceInstallments: Int? = null,
    val isInfinite: Boolean? = null,
    val invoiceMonth: Int? = null,
    val invoiceYear: Int? = null,
    val generatesDebt: Boolean? = null,
    val installmentAmountMode: String? = null,
    val remainderStrategy: InstallmentRemainderStrategy = InstallmentRemainderStrategy.FIRST_INSTALLMENT
)

data class InstallmentGroupItemRequest(
    val transactionId: UUID,
    val amount: Int
)

data class UpdateInstallmentGroupAmountsRequest(
    val items: List<InstallmentGroupItemRequest>
)
