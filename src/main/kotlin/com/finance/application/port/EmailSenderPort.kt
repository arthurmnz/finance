package com.finance.application.port

interface EmailSenderPort {
    fun sendPasswordResetEmail(toEmail: String, recipientName: String, resetLink: String)
}
