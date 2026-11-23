package com.finance.domain.service

import com.finance.domain.enum.InstallmentRemainderStrategy
import com.finance.domain.exception.InvalidTransactionException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class InstallmentAmountAllocatorTest {

    @Test
    fun `should allocate 19994 cents in 4 installments accurately for all 4 strategies`() {
        assertEquals(
            listOf(5000, 4998, 4998, 4998),
            InstallmentAmountAllocator.allocate(19994, 4, InstallmentRemainderStrategy.FIRST_INSTALLMENT)
        )
        assertEquals(
            listOf(4998, 4998, 4998, 5000),
            InstallmentAmountAllocator.allocate(19994, 4, InstallmentRemainderStrategy.LAST_INSTALLMENT)
        )
        assertEquals(
            listOf(4999, 4999, 4998, 4998),
            InstallmentAmountAllocator.allocate(19994, 4, InstallmentRemainderStrategy.DISTRIBUTE_FIRST)
        )
        assertEquals(
            listOf(4998, 4998, 4999, 4999),
            InstallmentAmountAllocator.allocate(19994, 4, InstallmentRemainderStrategy.DISTRIBUTE_LAST)
        )
    }

    @Test
    fun `should use FIRST_INSTALLMENT as default strategy and handle exact division`() {
        assertEquals(
            listOf(5000, 4998, 4998, 4998),
            InstallmentAmountAllocator.allocate(19994, 4)
        )

        for (strategy in InstallmentRemainderStrategy.entries) {
            assertEquals(
                listOf(2500, 2500, 2500, 2500),
                InstallmentAmountAllocator.allocate(10000, 4, strategy)
            )
        }
    }

    @Test
    fun `should support single installment and minimum 1 cent per installment`() {
        assertEquals(listOf(19994), InstallmentAmountAllocator.allocate(19994, 1))
        assertEquals(listOf(1, 1, 1, 1), InstallmentAmountAllocator.allocate(4, 4))

        val distributed = InstallmentAmountAllocator.allocate(1003, 4, InstallmentRemainderStrategy.DISTRIBUTE_FIRST)
        assertEquals(listOf(251, 251, 251, 250), distributed)
        assertEquals(1003, distributed.sum())

        val distributedLast = InstallmentAmountAllocator.allocate(1003, 4, InstallmentRemainderStrategy.DISTRIBUTE_LAST)
        assertEquals(listOf(250, 251, 251, 251), distributedLast)
        assertEquals(1003, distributedLast.sum())
    }

    @Test
    fun `should throw InvalidTransactionException when count is invalid or totalCents is less than count`() {
        assertThrows<InvalidTransactionException> {
            InstallmentAmountAllocator.allocate(1000, 0)
        }
        assertThrows<InvalidTransactionException> {
            InstallmentAmountAllocator.allocate(1000, -2)
        }
        assertThrows<InvalidTransactionException> {
            InstallmentAmountAllocator.allocate(3, 4)
        }
        assertThrows<InvalidTransactionException> {
            InstallmentAmountAllocator.allocate(0, 1)
        }
        assertThrows<InvalidTransactionException> {
            InstallmentAmountAllocator.allocate(-500, 2)
        }
    }
}
