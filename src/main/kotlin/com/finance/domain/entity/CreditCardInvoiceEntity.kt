package com.finance.domain.entity

import java.time.LocalDateTime
import java.util.UUID

class CreditCardInvoiceEntity(
    val id: UUID = UUID.randomUUID(),
    val creditCardId: UUID,
    val month: Int,
    val year: Int,
    paidAmount: Int = 0,
    carriedOverAmount: Int = 0,
    revolvingInterestAmount: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now()
) {
    var paidAmount: Int = paidAmount
        private set

    var carriedOverAmount: Int = carriedOverAmount
        private set

    var revolvingInterestAmount: Int = revolvingInterestAmount
        private set

    var updatedAt: LocalDateTime = updatedAt
        private set

    fun addPayment(amount: Int) {
        require(amount > 0) { "O valor do pagamento deve ser maior que zero." }
        this.paidAmount += amount
        this.updatedAt = LocalDateTime.now()
    }

    fun setCarriedOver(amount: Int) {
        this.carriedOverAmount = amount.coerceAtLeast(0)
        this.updatedAt = LocalDateTime.now()
    }

    fun setRevolvingInterest(amount: Int) {
        require(amount >= 0) { "O valor do rotativo não pode ser negativo." }
        this.revolvingInterestAmount = amount
        this.updatedAt = LocalDateTime.now()
    }
}
