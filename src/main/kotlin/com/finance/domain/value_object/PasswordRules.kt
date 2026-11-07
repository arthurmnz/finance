package com.finance.domain.value_object

object PasswordRules {

    fun validate(raw: String): String {
        require(raw.isNotBlank()) { "A senha não pode estar em branco." }
        require(raw.length >= 8) { "A senha deve ter no mínimo 8 caracteres." }
        require(raw.length <= 128) { "A senha deve ter no máximo 128 caracteres." }
        require(raw.any { it.isUpperCase() }) { "A senha deve conter pelo menos uma letra maiúscula." }
        require(raw.any { it.isLowerCase() }) { "A senha deve conter pelo menos uma letra minúscula." }
        require(raw.any { it.isDigit() }) { "A senha deve conter pelo menos um número." }
        require(raw.any { !it.isLetterOrDigit() }) { "A senha deve conter pelo menos um caractere especial." }
        return raw
    }
}
