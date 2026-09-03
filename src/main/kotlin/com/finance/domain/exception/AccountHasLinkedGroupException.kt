package com.finance.domain.exception

class AccountHasLinkedGroupException(message: String = "Existem contas vinculadas ao grupo. Desvincule-as antes de sair.") : RuntimeException(message)
