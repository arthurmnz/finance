package com.finance.application.service

import com.finance.domain.entity.CreditCardInvoiceEntity
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.repository.CreditCardInvoiceRepository
import com.finance.domain.repository.CreditCardRepository

object InvoicePaymentTransactionResolver {
    private val PAYMENT_TITLE_REGEX = Regex("""^Pagamento fatura (.+?) \((\d{1,2})/(\d{4})\)$""")

    fun findInvoiceForPaymentTx(
        tx: TransactionEntity,
        creditCardInvoiceRepository: CreditCardInvoiceRepository,
        creditCardRepository: CreditCardRepository
    ): CreditCardInvoiceEntity? {
        if (tx.creditCardInvoiceId != null) {
            return creditCardInvoiceRepository.findById(tx.creditCardInvoiceId)
        }
        val match = PAYMENT_TITLE_REGEX.find(tx.title) ?: return null
        val (cardName, monthStr, yearStr) = match.destructured
        val month = monthStr.toIntOrNull() ?: return null
        val year = yearStr.toIntOrNull() ?: return null

        val cards = creditCardRepository.findAllByOwnerId(tx.responsibleId)
        val card = cards.find { it.name.equals(cardName.trim(), ignoreCase = true) } ?: return null
        return creditCardInvoiceRepository.findByCardAndPeriod(card.id, month, year)
    }
}
