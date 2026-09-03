package com.finance.presentation.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(
    info = Info(
        title = "Finance API",
        version = "v1",
        description = "API de Finanças construída com Clean Architecture, autenticação JWT stateless e Refresh Token com Rotação (RTT)."
    ),
    security = [
        SecurityRequirement(name = "BearerAuth")
    ]
)
@SecurityScheme(
    name = "BearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Insira o access token JWT retornado no /api/v1/auth/login para autenticar as rotas protegidas."
)
class OpenApiConfig
