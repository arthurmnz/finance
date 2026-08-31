package com.finance.domain.value_object

data class Name(val value: String) {
    init {
        val trimmed = value.trim()
        require(trimmed.isNotBlank()) { "O nome não pode estar em branco." }
        require(trimmed.length in 2..100) { "O nome deve ter entre 2 e 100 caracteres." }
    }

    val formatted: String
        get() = value.trim()

    override fun toString(): String = formatted
}
