package com.finance.domain.value_object

data class PasswordHash(val value: String) {
    init {
        require(value.isNotBlank()) { "O hash da senha não pode estar em branco." }
    }

    override fun toString(): String = "[PROTECTED]"
}
