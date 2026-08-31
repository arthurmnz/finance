package com.finance.domain.exception

class AccessDeniedException(message: String = "Acesso negado para esta operação.") : RuntimeException(message)
