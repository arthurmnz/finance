package com.finance.application.service

import com.finance.domain.exception.InvalidTransactionException

object TransactionInputValidator {
    const val MIN_INVOICE_YEAR = 1900
    const val MAX_INVOICE_YEAR = 2100
    const val MAX_RECURRENCE_INSTALLMENTS = 365
    const val MAX_RECURRENCE_INTERVAL = 120

    fun validateInvoicePeriod(invoiceMonth: Int?, invoiceYear: Int?) {
        if (invoiceMonth != null && invoiceMonth !in 1..12) {
            throw InvalidTransactionException("O mês da fatura deve estar entre 1 e 12.")
        }
        if (invoiceYear != null && invoiceYear !in MIN_INVOICE_YEAR..MAX_INVOICE_YEAR) {
            throw InvalidTransactionException("O ano da fatura deve estar entre $MIN_INVOICE_YEAR e $MAX_INVOICE_YEAR.")
        }
    }

    fun validateRecurrenceLimits(recurrenceInstallments: Int?, recurrenceInterval: Int?) {
        if (recurrenceInstallments != null && recurrenceInstallments !in 0..MAX_RECURRENCE_INSTALLMENTS) {
            throw InvalidTransactionException(
                "A quantidade de parcelas deve estar entre 0 e $MAX_RECURRENCE_INSTALLMENTS."
            )
        }
        if (recurrenceInterval != null && recurrenceInterval !in 1..MAX_RECURRENCE_INTERVAL) {
            throw InvalidTransactionException(
                "O intervalo de recorrência deve estar entre 1 e $MAX_RECURRENCE_INTERVAL."
            )
        }
    }
}
