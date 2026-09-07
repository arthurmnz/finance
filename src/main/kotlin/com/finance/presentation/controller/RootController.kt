package com.finance.presentation.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Health", description = "Endpoints de saúde da aplicação")
class RootController {

    @GetMapping("/")
    @Operation(summary = "Verificar status da aplicação")
    fun getStatus(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("status" to "UP","swagger" to "/swagger-ui/index.html"))
    }
}
