package com.finance.domain.exception

class CreditCardNotFoundException(message: String = "Cartão de crédito não encontrado.") : RuntimeException(message)
