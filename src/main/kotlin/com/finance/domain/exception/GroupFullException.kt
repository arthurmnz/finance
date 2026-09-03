package com.finance.domain.exception

class GroupFullException(message: String = "O grupo atingiu o limite máximo de membros (10).") : RuntimeException(message)
