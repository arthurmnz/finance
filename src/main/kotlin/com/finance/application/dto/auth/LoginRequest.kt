package com.finance.application.dto.auth

data class LoginRequest(
    val email: String,
    val password: String,
)
