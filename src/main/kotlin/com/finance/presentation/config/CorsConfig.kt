package com.finance.presentation.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
class CorsConfig(
    @Value("\${cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private val allowedOriginsRaw: String
) {
    @Bean
    fun corsFilter(): CorsFilter {
        val origins = allowedOriginsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration()
        config.allowCredentials = true
        config.allowedOriginPatterns = origins.ifEmpty { listOf("http://localhost:5173") }
        config.allowedHeaders = listOf("*")
        config.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD")
        source.registerCorsConfiguration("/**", config)
        return CorsFilter(source)
    }
}
