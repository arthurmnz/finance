package com.finance.domain.entity

import com.finance.domain.exception.InvalidTransactionException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class DebtSettlementEntity(
    val id: UUID,
    val groupId: UUID,
    val debtorId: UUID,
    val creditorId: UUID,
    amount: Int,
    targetMonth: Int,
    targetYear: Int,
    val date: LocalDate = LocalDate.now(),
    val transactionId: UUID? = null,
    notes: String? = null,
    val isForgiveness: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now()
) {
    var amount: Int = amount
        private set

    var targetMonth: Int = targetMonth
        private set

    var targetYear: Int = targetYear
        private set

    var notes: String? = notes
        private set

    var updatedAt: LocalDateTime = updatedAt
        private set

    init {
        require(amount > 0) { "O valor do acerto deve ser maior que zero." }
        require(debtorId != creditorId) { "O devedor e o credor não podem ser a mesma pessoa." }
        require(targetMonth in 1..12) { "Mês alvo inválido." }
        if (targetYear !in 1970..2100) {
            throw InvalidTransactionException("Ano de referência inválido.")
        }
    }

    constructor(
        groupId: UUID,
        debtorId: UUID,
        creditorId: UUID,
        amount: Int,
        targetMonth: Int,
        targetYear: Int,
        date: LocalDate = LocalDate.now(),
        transactionId: UUID? = null,
        notes: String? = null,
        isForgiveness: Boolean = false
    ) : this(
        id = UUID.randomUUID(),
        groupId = groupId,
        debtorId = debtorId,
        creditorId = creditorId,
        amount = amount,
        targetMonth = targetMonth,
        targetYear = targetYear,
        date = date,
        transactionId = transactionId,
        notes = notes,
        isForgiveness = isForgiveness,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DebtSettlementEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
