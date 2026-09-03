package com.finance.infraestructure.security

import com.finance.application.port.TokenProviderPort
import com.finance.domain.entity.UserEntity
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenProviderAdapter(
    @Value("\${jwt.secret:finance_super_secret_jwt_key_that_must_be_at_least_256_bits_long_123456}")
    private val jwtSecret: String,

    @Value("\${jwt.access-token-expiration-minutes:15}")
    private val accessTokenExpirationMinutes: Long,

    @Value("\${jwt.refresh-token-expiration-days:7}")
    private val refreshTokenExpirationDays: Long,
) : TokenProviderPort {

    private val secretKey: SecretKey by lazy {
        val keyBytes = jwtSecret.padEnd(32, '0').toByteArray(StandardCharsets.UTF_8)
        Keys.hmacShaKeyFor(keyBytes)
    }

    private val secureRandom = SecureRandom()

    override fun generateAccessToken(user: UserEntity): String {
        val now = Instant.now()
        val expiration = now.plus(Duration.ofMinutes(accessTokenExpirationMinutes))

        return Jwts.builder()
            .subject(user.email.normalized)
            .claim("name", user.fullName())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(secretKey)
            .compact()
    }

    override fun generateRefreshToken(): String {
        val randomBytes = ByteArray(48)
        secureRandom.nextBytes(randomBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
    }

    override fun validateAndGetUserEmail(accessToken: String): String? {
        return try {
            val claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(accessToken)
                .payload
            claims.subject
        } catch (e: Exception) {
            null
        }
    }

    override fun getAccessTokenExpirationSeconds(): Long {
        return accessTokenExpirationMinutes * 60
    }

    override fun getRefreshTokenDuration(): Duration {
        return Duration.ofDays(refreshTokenExpirationDays)
    }
}
