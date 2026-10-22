package com.finance.domain.entity

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class CreditCardEntity(
    val id: UUID,
    val ownerId: UUID,
    accountId: UUID,
    name: String,
    limitAmount: Int,
    closingDay: Int,
    dueDay: Int,
    groupId: UUID?,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now()
) {
    var accountId: UUID = accountId
        private set

    var name: String = name
        private set

    var limitAmount: Int = limitAmount
        private set

    var closingDay: Int = closingDay
        private set

    var dueDay: Int = dueDay
        private set

    var groupId: UUID? = groupId
        private set

    var updatedAt: LocalDateTime = updatedAt
        private set

    init {
        validate(name, limitAmount, closingDay, dueDay)
    }

    constructor(
        ownerId: UUID,
        accountId: UUID,
        name: String,
        limitAmount: Int,
        closingDay: Int,
        dueDay: Int
    ) : this(
        id = UUID.randomUUID(),
        ownerId = ownerId,
        accountId = accountId,
        name = name.trim(),
        limitAmount = limitAmount,
        closingDay = closingDay,
        dueDay = dueDay,
        groupId = null
    )

    fun update(newName: String, newAccountId: UUID, newLimitAmount: Int, newClosingDay: Int, newDueDay: Int) {
        validate(newName, newLimitAmount, newClosingDay, newDueDay)
        this.name = newName.trim()
        this.accountId = newAccountId
        this.limitAmount = newLimitAmount
        this.closingDay = newClosingDay
        this.dueDay = newDueDay
        this.updatedAt = LocalDateTime.now()
    }

    fun linkToGroup(newGroupId: UUID) {
        check(this.groupId == null) { "Cartão já está vinculado a um grupo." }
        this.groupId = newGroupId
        this.updatedAt = LocalDateTime.now()
    }

    fun unlinkFromGroup() {
        this.groupId = null
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * Calcula a fatura (mês, ano) padrão de uma compra realizada em [purchaseDate].
     * Regra: compras realizadas antes do dia de fechamento entram na fatura corrente;
     * compras no dia de fechamento ou após entram na próxima fatura.
     * Se dueDay < closingDay, a fatura fechada no mês M vence no mês M+1.
     */
    fun defaultInvoiceMonthYear(purchaseDate: LocalDate): YearMonth {
        val baseMonth = if (purchaseDate.dayOfMonth >= closingDay) {
            YearMonth.from(purchaseDate).plusMonths(1)
        } else {
            YearMonth.from(purchaseDate)
        }
        return if (dueDay < closingDay) baseMonth.plusMonths(1) else baseMonth
    }

    companion object {
        private fun validate(name: String, limitAmount: Int, closingDay: Int, dueDay: Int) {
            require(name.isNotBlank()) { "Nome do cartão não pode ser vazio." }
            require(limitAmount >= 0) { "Limite não pode ser negativo." }
            require(closingDay in 1..31) { "Dia de fechamento deve estar entre 1 e 31." }
            require(dueDay in 1..31) { "Dia de vencimento deve estar entre 1 e 31." }
        }
    }
}
