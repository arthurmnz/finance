package com.finance.domain.exception

class TransactionNotFoundException(message: String = "Transaction not found") : RuntimeException(message)
