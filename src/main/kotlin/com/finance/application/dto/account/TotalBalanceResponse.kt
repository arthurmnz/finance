package com.finance.application.dto.account

import java.math.BigDecimal

data class TotalBalanceResponse(
    val total: Int,
    val monthlyIncome: Int,
    val monthlyExpense: Int
)
