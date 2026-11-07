package com.finance.presentation.controller

import com.finance.application.dto.auth.AuthResponse
import com.finance.application.dto.auth.ForgotPasswordRequest
import com.finance.application.dto.auth.LoginRequest
import com.finance.application.dto.auth.MessageResponse
import com.finance.application.dto.auth.RegisterUserRequest
import com.finance.application.dto.auth.RegisterUserResponse
import com.finance.application.dto.auth.ResetPasswordRequest
import com.finance.application.port.TokenProviderPort
import com.finance.application.use_case.auth.ForgotPasswordUseCase
import com.finance.application.use_case.auth.LoginUseCase
import com.finance.application.use_case.auth.LogoutUseCase
import com.finance.application.use_case.auth.RefreshTokenUseCase
import com.finance.application.use_case.auth.RegisterUserUseCase
import com.finance.application.use_case.auth.ResetPasswordUseCase
import com.finance.infraestructure.security.RateLimiterService
import com.finance.presentation.util.AuthCookieUtil
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Endpoints de autenticação, tokens e registro")
class AuthController(
    private val registerUserUseCase: RegisterUserUseCase,
    private val loginUseCase: LoginUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val rateLimiterService: RateLimiterService,
    private val tokenProvider: TokenProviderPort,
    private val authCookieUtil: AuthCookieUtil,
) {

    @PostMapping("/register")
    @SecurityRequirements
    @Operation(summary = "Registrar novo usuário")
    fun register(@RequestBody request: RegisterUserRequest): ResponseEntity<RegisterUserResponse> {
        val response = registerUserUseCase.execute(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Autenticar usuário, gerar access token e definir cookie HttpOnly com refresh token")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        val result = loginUseCase.execute(request)
        val cookie = authCookieUtil.createRefreshTokenCookie(
            token = result.refreshToken,
            duration = tokenProvider.getRefreshTokenDuration()
        )

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(result.toAuthResponse())
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Rotacionar refresh token e renovar access token via cookie HttpOnly")
    fun refresh(
        @Parameter(description = "Refresh token enviado automaticamente pelo navegador via cookie HttpOnly 'refreshToken'")
        @CookieValue(name = AuthCookieUtil.REFRESH_TOKEN_COOKIE_NAME, required = false) refreshToken: String?,
    ): ResponseEntity<AuthResponse> {
        val result = refreshTokenUseCase.execute(refreshToken)
        val cookie = authCookieUtil.createRefreshTokenCookie(
            token = result.refreshToken,
            duration = tokenProvider.getRefreshTokenDuration()
        )

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(result.toAuthResponse())
    }

    @PostMapping("/logout")
    @SecurityRequirements
    @Operation(summary = "Encerrar sessão e revogar refresh token")
    fun logout(
        @Parameter(description = "Refresh token enviado automaticamente pelo navegador via cookie HttpOnly 'refreshToken'")
        @CookieValue(name = AuthCookieUtil.REFRESH_TOKEN_COOKIE_NAME, required = false) refreshToken: String?,
    ): ResponseEntity<Void> {
        logoutUseCase.execute(refreshToken)
        val clearCookie = authCookieUtil.createEmptyRefreshTokenCookie()

        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
            .build()
    }

    @PostMapping("/forgot-password")
    @SecurityRequirements
    @Operation(summary = "Solicitar link de recuperação de senha por e-mail")
    fun forgotPassword(
        @RequestBody request: ForgotPasswordRequest,
        servletRequest: HttpServletRequest,
    ): ResponseEntity<MessageResponse> {
        val clientIp = rateLimiterService.resolveClientIp(servletRequest)
        rateLimiterService.checkRateLimit(action = "forgot-password", clientIp = clientIp)
        val response = forgotPasswordUseCase.execute(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/reset-password")
    @SecurityRequirements
    @Operation(summary = "Redefinir senha com token recebido por e-mail")
    fun resetPassword(
        @RequestBody request: ResetPasswordRequest,
        servletRequest: HttpServletRequest,
    ): ResponseEntity<MessageResponse> {
        val clientIp = rateLimiterService.resolveClientIp(servletRequest)
        rateLimiterService.checkRateLimit(action = "reset-password", clientIp = clientIp)
        val response = resetPasswordUseCase.execute(request)
        return ResponseEntity.ok(response)
    }
}
