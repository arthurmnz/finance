package com.finance.domain.enum

/**
 * Estratégia de alocação do resto da divisão inteira em centavos
 * quando um valor total é dividido em parcelas.
 */
enum class InstallmentRemainderStrategy {
    /** Soma todo o resto na primeira parcela. */
    FIRST_INSTALLMENT,

    /** Soma todo o resto na última parcela. */
    LAST_INSTALLMENT,

    /** Distribui 1 centavo por parcela a partir da primeira até zerar o resto. */
    DISTRIBUTE_FIRST,

    /** Distribui 1 centavo por parcela a partir das últimas até zerar o resto. */
    DISTRIBUTE_LAST
}
