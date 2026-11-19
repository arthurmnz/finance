package com.finance.domain.entity

import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.InvalidTransactionException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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

    @Test
    fun `should store and update generalCategoryId while keeping categoryId compatibility`() {
        val generalId = UUID.randomUUID()
        val specificId = UUID.randomUUID()
        val newGeneralId = UUID.randomUUID()

        val tx = TransactionEntity(
            title = "Conta de Luz",
            amount = 20000,
            date = LocalDate.of(2026, 11, 10),
            responsibleId = UUID.randomUUID(),
            accountId = UUID.randomUUID(),
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = specificId,
            status = TransactionStatus.COMPLETED,
            generalCategoryId = generalId
        )
        assertEquals(specificId, tx.categoryId)
        assertEquals(generalId, tx.generalCategoryId)

        // Updating without specifying newGeneralCategoryId preserves existing generalCategoryId
        tx.update(
            newTitle = "Conta de Luz Atualizada",
            newAmount = 21000,
            newDate = LocalDate.of(2026, 11, 11),
            newCategoryId = specificId,
            newStatus = TransactionStatus.COMPLETED
        )
        assertEquals(generalId, tx.generalCategoryId)

        // Updating with newGeneralCategoryId updates both
        tx.update(
            newTitle = "Só Geral",
            newAmount = 21000,
            newDate = LocalDate.of(2026, 11, 11),
            newCategoryId = newGeneralId,
            newStatus = TransactionStatus.COMPLETED,
            newGeneralCategoryId = newGeneralId
        )
        assertEquals(newGeneralId, tx.categoryId)
        assertEquals(newGeneralId, tx.generalCategoryId)

        // Clearing both to null
        tx.update(
            newTitle = "Sem Categoria",
            newAmount = 21000,
            newDate = LocalDate.of(2026, 11, 11),
            newCategoryId = null,
            newStatus = TransactionStatus.COMPLETED,
            newGeneralCategoryId = null
        )
        assertNull(tx.categoryId)
        assertNull(tx.generalCategoryId)
    }
}
