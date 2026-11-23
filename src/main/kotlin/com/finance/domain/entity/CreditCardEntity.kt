package com.finance.domain.entity

import com.finance.domain.enum.RecurrenceFrequency
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID

class CreditCardEntity(
    val id: UUID,
    val ownerId: UUID,
    accountId: UUID,
    name: String,
    limitAmount: Int,
    closingDay: Int,
    dueDay: Int,
    groupId: UUID? = null,
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

    /**
     * Calcula a data de fechamento da fatura [invoiceYm],
     * limitando [closingDay] ao último dia do mês quando necessário.
     */
    fun invoiceClosingDate(invoiceYm: YearMonth): LocalDate =
        closingDayOn(closingYearMonth(invoiceYm))

    /**
     * Calcula a data de abertura da fatura [invoiceYm] (data de fechamento da fatura anterior).
     */
    fun invoiceOpeningDate(invoiceYm: YearMonth): LocalDate =
        closingDayOn(closingYearMonth(invoiceYm).minusMonths(1))

    /**
     * Calcula a competência de fatura de uma parcela/ocorrência considerando o deslocamento [offset].
     */
    fun installmentInvoiceMonthYear(
        baseInvoiceYm: YearMonth,
        offset: Int,
        frequency: RecurrenceFrequency?,
        interval: Int,
        currentDate: LocalDate
    ): YearMonth {
        if (offset == 0) return baseInvoiceYm
        val monthsOffset = when (frequency) {
            RecurrenceFrequency.YEARLY -> (offset * interval * 12).toLong()
            RecurrenceFrequency.MONTHLY, null -> (offset * interval).toLong()
            RecurrenceFrequency.DAILY, RecurrenceFrequency.WEEKLY ->
                ChronoUnit.MONTHS.between(baseInvoiceYm, defaultInvoiceMonthYear(currentDate))
        }
        return baseInvoiceYm.plusMonths(monthsOffset)
    }

    /**
     * Define a data de lançamento de uma parcela futura (offset > 0) no cartão:
     * parcelas em faturas subsequentes usam a data de abertura da respectiva fatura.
     */
    fun installmentItemDate(
        baseInvoiceYm: YearMonth,
        itemInvoiceYm: YearMonth,
        frequency: RecurrenceFrequency?,
        fallbackDate: LocalDate
    ): LocalDate = when (frequency) {
        RecurrenceFrequency.DAILY, RecurrenceFrequency.WEEKLY ->
            if (itemInvoiceYm > baseInvoiceYm) invoiceOpeningDate(itemInvoiceYm) else fallbackDate
        RecurrenceFrequency.MONTHLY, RecurrenceFrequency.YEARLY, null ->
            invoiceOpeningDate(itemInvoiceYm)
    }

    private fun closingYearMonth(invoiceYm: YearMonth): YearMonth =
        if (dueDay < closingDay) invoiceYm.minusMonths(1) else invoiceYm

    private fun closingDayOn(ym: YearMonth): LocalDate =
        LocalDate.of(ym.year, ym.monthValue, closingDay.coerceAtMost(ym.lengthOfMonth()))

    companion object {
        private fun validate(name: String, limitAmount: Int, closingDay: Int, dueDay: Int) {
            require(name.isNotBlank()) { "Nome do cartão não pode ser vazio." }
            require(limitAmount >= 0) { "Limite não pode ser negativo." }
            require(closingDay in 1..31) { "Dia de fechamento deve estar entre 1 e 31." }
            require(dueDay in 1..31) { "Dia de vencimento deve estar entre 1 e 31." }
        }
    }
}
