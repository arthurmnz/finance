package com.finance.application.dto.auth

data class ForgotPasswordRequest(
    val email: String,
)

data class ResetPasswordRequest(
    val token: String,
    val newPassword: String,
)

data class MessageResponse(
    val message: String,
)
