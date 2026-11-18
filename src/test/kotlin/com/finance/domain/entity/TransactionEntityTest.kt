package com.finance.domain.entity

import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.InvalidTransactionException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransactionEntityTest {

    @Test
    fun `should allow generatesDebt true only for bank transfer`() {
        val tx = TransactionEntity(
            title = "PIX",
            amount = 10000,
            date = LocalDate.of(2026, 11, 10),
            responsibleId = UUID.randomUUID(),
            accountId = UUID.randomUUID(),
            type = TransactionType.TRANSFER,
            destinationAccountId = UUID.randomUUID(),
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            generatesDebt = true
        )
        assertTrue(tx.generatesDebt)

        tx.update(
            newTitle = "PIX editado",
            newAmount = 10000,
            newDate = LocalDate.of(2026, 11, 10),
            newCategoryId = null,
            newStatus = TransactionStatus.COMPLETED,
            newGeneratesDebt = false
        )
        assertFalse(tx.generatesDebt)
    }

    @Test
    fun `should reject generatesDebt true for non-transfer or credit card transactions`() {
        assertThrows<IllegalArgumentException> {
            TransactionEntity(
                title = "Compra",
                amount = 10000,
                date = LocalDate.of(2026, 11, 10),
                responsibleId = UUID.randomUUID(),
                accountId = UUID.randomUUID(),
                type = TransactionType.EXPENSE,
                destinationAccountId = null,
                categoryId = null,
                status = TransactionStatus.COMPLETED,
                generatesDebt = true
            )
        }

        val expense = TransactionEntity(
            title = "Compra",
            amount = 10000,
            date = LocalDate.of(2026, 11, 10),
            responsibleId = UUID.randomUUID(),
            accountId = UUID.randomUUID(),
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED
        )

        assertThrows<IllegalArgumentException> {
            expense.update(
                newTitle = "Compra",
                newAmount = 10000,
                newDate = LocalDate.of(2026, 11, 10),
                newCategoryId = null,
                newStatus = TransactionStatus.COMPLETED,
                newGeneratesDebt = true
            )
        }
    }

    @Test
    fun `should reject non-positive amount on init and update`() {
        assertThrows<InvalidTransactionException> {
            TransactionEntity(
                title = "Zerada",
                amount = 0,
                date = LocalDate.of(2026, 11, 10),
                responsibleId = UUID.randomUUID(),
                accountId = UUID.randomUUID(),
                type = TransactionType.EXPENSE,
                destinationAccountId = null,
                categoryId = null,
                status = TransactionStatus.COMPLETED
            )
        }

        assertThrows<InvalidTransactionException> {
            TransactionEntity(
                title = "Negativa",
                amount = -100,
                date = LocalDate.of(2026, 11, 10),
                responsibleId = UUID.randomUUID(),
                accountId = UUID.randomUUID(),
                type = TransactionType.EXPENSE,
                destinationAccountId = null,
                categoryId = null,
                status = TransactionStatus.COMPLETED
            )
        }

        val valid = TransactionEntity(
            title = "Valida",
            amount = 1000,
            date = LocalDate.of(2026, 11, 10),
            responsibleId = UUID.randomUUID(),
            accountId = UUID.randomUUID(),
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED
        )

        assertThrows<InvalidTransactionException> {
            valid.update(
                newTitle = "Valida",
                newAmount = 0,
                newDate = LocalDate.of(2026, 11, 10),
                newCategoryId = null,
                newStatus = TransactionStatus.COMPLETED
            )
        }

        assertThrows<InvalidTransactionException> {
            valid.update(
                newTitle = "Valida",
                newAmount = -50,
                newDate = LocalDate.of(2026, 11, 10),
                newCategoryId = null,
                newStatus = TransactionStatus.COMPLETED
            )
        }
    }
}
