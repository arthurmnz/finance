package com.finance.domain.exception

class AccountAlreadyLinkedException(message: String = "Esta conta já está vinculada a um grupo.") : RuntimeException(message)
