package com.finance.domain.repository

import com.finance.domain.entity.CreditCardInvoiceEntity
import java.util.UUID

interface CreditCardInvoiceRepository {
    fun save(invoice: CreditCardInvoiceEntity): CreditCardInvoiceEntity
    fun findById(id: UUID): CreditCardInvoiceEntity?
    fun findByCardAndPeriod(creditCardId: UUID, month: Int, year: Int): CreditCardInvoiceEntity?
    fun findAllByCreditCardId(creditCardId: UUID): List<CreditCardInvoiceEntity>
    fun deleteAllByCreditCardId(creditCardId: UUID)
}
