package com.finance.domain.exception

class AccountGroupRequestAlreadyExistsException(message: String = "Já existe uma solicitação pendente para esta conta.") : RuntimeException(message)
