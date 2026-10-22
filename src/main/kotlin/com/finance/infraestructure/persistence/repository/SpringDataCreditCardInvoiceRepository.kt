package com.finance.infraestructure.persistence.repository

import com.finance.infraestructure.persistence.entity.CreditCardInvoiceJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SpringDataCreditCardInvoiceRepository : JpaRepository<CreditCardInvoiceJpaEntity, UUID> {
    fun findByCreditCardIdAndMonthAndYear(creditCardId: UUID, month: Int, year: Int): CreditCardInvoiceJpaEntity?
    fun findAllByCreditCardId(creditCardId: UUID): List<CreditCardInvoiceJpaEntity>
    fun deleteAllByCreditCardId(creditCardId: UUID)
}
