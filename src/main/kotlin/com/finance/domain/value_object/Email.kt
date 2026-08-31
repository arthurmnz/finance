package com.finance.domain.value_object

data class Email(val value: String) {
    companion object {
        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

        fun isValid(email: String): Boolean = EMAIL_REGEX.matches(email.trim())
    }

    val normalized: String

    init {
        val trimmed = value.trim()
        require(trimmed.isNotBlank()) { "O e-mail não pode estar em branco." }
        require(EMAIL_REGEX.matches(trimmed)) { "Formato de e-mail inválido: $value" }
        normalized = trimmed.lowercase()
    }

    override fun toString(): String = normalized
}
