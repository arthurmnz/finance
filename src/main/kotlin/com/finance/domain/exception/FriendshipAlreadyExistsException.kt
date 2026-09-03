package com.finance.domain.exception

class FriendshipAlreadyExistsException(message: String = "Já existe uma solicitação de amizade entre esses usuários.") : RuntimeException(message)
