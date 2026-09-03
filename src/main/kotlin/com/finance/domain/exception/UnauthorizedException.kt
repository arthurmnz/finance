package com.finance.domain.exception

class UnauthorizedException(message: String = "Não autorizado.") : RuntimeException(message)
