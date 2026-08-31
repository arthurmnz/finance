package com.finance.application.dto.user

data class UpdateUserRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
)
