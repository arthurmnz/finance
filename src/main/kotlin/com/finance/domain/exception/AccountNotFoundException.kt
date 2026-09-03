package com.finance.domain.exception

class AccountNotFoundException(message: String = "Conta não encontrada.") : RuntimeException(message)
