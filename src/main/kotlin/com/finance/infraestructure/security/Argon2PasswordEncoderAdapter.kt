package com.finance.infraestructure.security

import com.finance.application.port.PasswordEncoderPort
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.stereotype.Component

@Component
class Argon2PasswordEncoderAdapter : PasswordEncoderPort {

    private val encoder: Argon2PasswordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()

    override fun encode(rawPassword: String): String {
        return checkNotNull(encoder.encode(rawPassword)) { "Falha ao gerar hash da senha com Argon2." }
    }

    override fun matches(rawPassword: String, encodedPassword: String): Boolean {
        return encoder.matches(rawPassword, encodedPassword)
    }
}
