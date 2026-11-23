package com.finance.domain.entity

import com.finance.domain.enum.RecurrenceFrequency
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.test.assertEquals

class CreditCardEntityTest {

    private fun buildCard(closingDay: Int, dueDay: Int): CreditCardEntity = CreditCardEntity(
        id = UUID.randomUUID(),
        ownerId = UUID.randomUUID(),
        accountId = UUID.randomUUID(),
        name = "Cartão Teste",
        limitAmount = 500000,
        closingDay = closingDay,
        dueDay = dueDay
    )

    @Test
    fun `should compute closing and opening dates when dueDay is greater than or equal to closingDay`() {
        val card = buildCard(closingDay = 10, dueDay = 17)

        assertEquals(LocalDate.of(2026, 1, 10), card.invoiceClosingDate(YearMonth.of(2026, 1)))
        assertEquals(LocalDate.of(2025, 12, 10), card.invoiceOpeningDate(YearMonth.of(2026, 1)))

        assertEquals(LocalDate.of(2026, 2, 10), card.invoiceClosingDate(YearMonth.of(2026, 2)))
        assertEquals(LocalDate.of(2026, 1, 10), card.invoiceOpeningDate(YearMonth.of(2026, 2)))
    }

    @Test
    fun `should compute closing and opening dates when dueDay is less than closingDay`() {
        val card = buildCard(closingDay = 10, dueDay = 5)

        // Fatura 02/2026 fecha em 10/01/2026 e abre em 10/12/2025
        assertEquals(LocalDate.of(2026, 1, 10), card.invoiceClosingDate(YearMonth.of(2026, 2)))
        assertEquals(LocalDate.of(2025, 12, 10), card.invoiceOpeningDate(YearMonth.of(2026, 2)))

        // Fatura 03/2026 fecha em 10/02/2026 e abre em 10/01/2026
        assertEquals(LocalDate.of(2026, 2, 10), card.invoiceClosingDate(YearMonth.of(2026, 3)))
        assertEquals(LocalDate.of(2026, 1, 10), card.invoiceOpeningDate(YearMonth.of(2026, 3)))
    }

    @Test
    fun `should clamp closingDay 31 on short and leap months and return to 31 in March`() {
        val sameMonthCard = buildCard(closingDay = 31, dueDay = 31)

        // Ano não bissexto (2026)
        assertEquals(LocalDate.of(2026, 2, 28), sameMonthCard.invoiceClosingDate(YearMonth.of(2026, 2)))
        assertEquals(LocalDate.of(2026, 2, 28), sameMonthCard.invoiceOpeningDate(YearMonth.of(2026, 3)))
        assertEquals(LocalDate.of(2026, 3, 31), sameMonthCard.invoiceOpeningDate(YearMonth.of(2026, 4)))

        // Ano bissexto (2028)
        assertEquals(LocalDate.of(2028, 2, 29), sameMonthCard.invoiceClosingDate(YearMonth.of(2028, 2)))
        assertEquals(LocalDate.of(2028, 2, 29), sameMonthCard.invoiceOpeningDate(YearMonth.of(2028, 3)))
        assertEquals(LocalDate.of(2028, 3, 31), sameMonthCard.invoiceOpeningDate(YearMonth.of(2028, 4)))

        // Com dueDay < closingDay
        val nextMonthDueCard = buildCard(closingDay = 31, dueDay = 10)
        assertEquals(LocalDate.of(2026, 2, 28), nextMonthDueCard.invoiceClosingDate(YearMonth.of(2026, 3)))
        assertEquals(LocalDate.of(2026, 2, 28), nextMonthDueCard.invoiceOpeningDate(YearMonth.of(2026, 4)))
        assertEquals(LocalDate.of(2026, 3, 31), nextMonthDueCard.invoiceOpeningDate(YearMonth.of(2026, 5)))
        assertEquals(LocalDate.of(2028, 2, 29), nextMonthDueCard.invoiceOpeningDate(YearMonth.of(2028, 4)))
        assertEquals(LocalDate.of(2028, 3, 31), nextMonthDueCard.invoiceOpeningDate(YearMonth.of(2028, 5)))
    }

    @Test
    fun `should compute defaultInvoiceMonthYear and installmentItemDate consistently`() {
        val card = buildCard(closingDay = 10, dueDay = 17)
        val baseYm = card.defaultInvoiceMonthYear(LocalDate.of(2025, 12, 1))
        assertEquals(YearMonth.of(2025, 12), baseYm)

        assertEquals(
            LocalDate.of(2025, 12, 8),
            card.installmentItemDate(baseYm, YearMonth.of(2025, 12), RecurrenceFrequency.WEEKLY, LocalDate.of(2025, 12, 8))
        )
        assertEquals(
            LocalDate.of(2025, 12, 10),
            card.installmentItemDate(baseYm, YearMonth.of(2026, 1), RecurrenceFrequency.WEEKLY, LocalDate.of(2025, 12, 15))
        )
        assertEquals(
            LocalDate.of(2025, 12, 10),
            card.installmentItemDate(baseYm, YearMonth.of(2026, 1), RecurrenceFrequency.MONTHLY, LocalDate.of(2026, 1, 1))
        )
    }
}
