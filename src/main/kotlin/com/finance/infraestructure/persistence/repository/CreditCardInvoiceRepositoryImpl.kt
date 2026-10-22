package com.finance.infraestructure.persistence.repository

import com.finance.domain.entity.CreditCardInvoiceEntity
import com.finance.domain.repository.CreditCardInvoiceRepository
import com.finance.infraestructure.persistence.mapper.CreditCardMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
class CreditCardInvoiceRepositoryImpl(
    private val jpa: SpringDataCreditCardInvoiceRepository,
    private val mapper: CreditCardMapper
) : CreditCardInvoiceRepository {
    @Transactional
    override fun save(invoice: CreditCardInvoiceEntity): CreditCardInvoiceEntity =
        mapper.toInvoiceDomain(jpa.save(mapper.toInvoiceJpa(invoice)))

    @Transactional(readOnly = true)
    override fun findByCardAndPeriod(creditCardId: UUID, month: Int, year: Int): CreditCardInvoiceEntity? =
        jpa.findByCreditCardIdAndMonthAndYear(creditCardId, month, year)?.let { mapper.toInvoiceDomain(it) }

    @Transactional(readOnly = true)
    override fun findAllByCreditCardId(creditCardId: UUID): List<CreditCardInvoiceEntity> =
        jpa.findAllByCreditCardId(creditCardId).map { mapper.toInvoiceDomain(it) }

    @Transactional
    override fun deleteAllByCreditCardId(creditCardId: UUID) = jpa.deleteAllByCreditCardId(creditCardId)
}
