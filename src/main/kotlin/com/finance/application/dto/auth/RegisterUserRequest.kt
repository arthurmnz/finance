package com.finance.application.dto.auth

data class RegisterUserRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
)
