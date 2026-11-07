package com.finance.application.event

data class PasswordResetRequestedEvent(
    val toEmail: String,
    val recipientName: String,
    val resetLink: String,
)
