package com.finance.domain.exception

class UserNotFoundException(message: String = "Usuário não encontrado.") : RuntimeException(message)
