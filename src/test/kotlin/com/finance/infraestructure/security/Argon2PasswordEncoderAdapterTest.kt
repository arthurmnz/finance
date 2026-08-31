package com.finance.infraestructure.security

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class Argon2PasswordEncoderAdapterTest {

    private val passwordEncoder = Argon2PasswordEncoderAdapter()

    @Test
    fun `should encode password and verify matches`() {
        val rawPassword = "SecurePassword@123"
        val encoded = passwordEncoder.encode(rawPassword)

        assertNotEquals(rawPassword, encoded)
        assertTrue(encoded.startsWith("\$argon2"))
        assertTrue(passwordEncoder.matches(rawPassword, encoded))
        assertFalse(passwordEncoder.matches("WrongPassword", encoded))
    }
}
