package com.finance.application.dto.account

import com.finance.domain.enum.AccountType
import java.math.BigDecimal

data class CreateAccountRequest(
    val name: String,
    val startBalance: Int,
    val type: AccountType,
    val tax: Int? = null
)
