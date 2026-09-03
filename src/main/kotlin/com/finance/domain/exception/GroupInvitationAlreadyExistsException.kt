package com.finance.domain.exception

class GroupInvitationAlreadyExistsException(message: String = "Já existe um convite pendente para este usuário neste grupo.") : RuntimeException(message)
