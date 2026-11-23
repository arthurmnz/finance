package com.finance.domain.service

import com.finance.domain.enum.InstallmentRemainderStrategy
import com.finance.domain.exception.InvalidTransactionException

/**
 * Utilitário de domínio para divisão exata de um valor total em centavos
 * em parcelas inteiras, garantindo que a soma das parcelas corresponda
 * exatamente ao total informado.
 */
object InstallmentAmountAllocator {

    fun allocate(
        totalCents: Int,
        count: Int,
        strategy: InstallmentRemainderStrategy = InstallmentRemainderStrategy.FIRST_INSTALLMENT
    ): List<Int> {
        if (count < 1) {
            throw InvalidTransactionException("A quantidade de parcelas deve ser pelo menos 1.")
        }
        if (totalCents < count) {
            throw InvalidTransactionException(
                "O valor total em centavos ($totalCents) deve ser maior ou igual à quantidade de parcelas ($count)."
            )
        }

        val base = totalCents / count
        val rem = totalCents % count

        return List(count) { i ->
            when (strategy) {
                InstallmentRemainderStrategy.FIRST_INSTALLMENT -> if (i == 0) base + rem else base
                InstallmentRemainderStrategy.LAST_INSTALLMENT -> if (i == count - 1) base + rem else base
                InstallmentRemainderStrategy.DISTRIBUTE_FIRST -> if (i < rem) base + 1 else base
                InstallmentRemainderStrategy.DISTRIBUTE_LAST -> if (i >= count - rem) base + 1 else base
            }
        }
    }
}
