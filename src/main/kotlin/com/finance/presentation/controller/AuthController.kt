package com.finance.presentation.controller

import com.finance.application.dto.auth.RegisterUserRequest
import com.finance.application.dto.auth.RegisterUserResponse
import com.finance.application.use_case.auth.RegisterUserUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Endpoints de autenticação e registro")
class AuthController(
    private val registerUserUseCase: RegisterUserUseCase,
) {

    @PostMapping("/register")
    @Operation(summary = "Registrar novo usuário")
    fun register(@RequestBody request: RegisterUserRequest): ResponseEntity<RegisterUserResponse> {
        val response = registerUserUseCase.execute(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}
