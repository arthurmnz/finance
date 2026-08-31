package com.finance.domain.exception

class UserAlreadyExistsException(message: String = "Já existe um usuário cadastrado com este e-mail.") : RuntimeException(message)
