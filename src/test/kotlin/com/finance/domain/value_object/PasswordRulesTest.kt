package com.finance.domain.value_object

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PasswordRulesTest {

    @Test
    fun `should accept valid strong password`() {
        val valid = "StrongPass123!"
        val result = PasswordRules.validate(valid)
        assertEquals(valid, result)
    }

    @Test
    fun `should reject blank password`() {
        val ex = assertThrows<IllegalArgumentException> {
            PasswordRules.validate("   ")
        }
        assertTrue(ex.message!!.contains("em branco"))
    }

    @Test
    fun `should reject short password`() {
        val ex = assertThrows<IllegalArgumentException> {
            PasswordRules.validate("Ab1!xyz")
        }
        assertTrue(ex.message!!.contains("no mínimo 8"))
    }

    @Test
    fun `should reject password without uppercase`() {
        val ex = assertThrows<IllegalArgumentException> {
            PasswordRules.validate("strongpass123!")
        }
        assertTrue(ex.message!!.contains("maiúscula"))
    }

    @Test
    fun `should reject password without lowercase`() {
        val ex = assertThrows<IllegalArgumentException> {
            PasswordRules.validate("STRONGPASS123!")
        }
        assertTrue(ex.message!!.contains("minúscula"))
    }

    @Test
    fun `should reject password without digit`() {
        val ex = assertThrows<IllegalArgumentException> {
            PasswordRules.validate("StrongPassword!")
        }
        assertTrue(ex.message!!.contains("número"))
    }

    @Test
    fun `should reject password without special char`() {
        val ex = assertThrows<IllegalArgumentException> {
            PasswordRules.validate("StrongPass1234")
        }
        assertTrue(ex.message!!.contains("caractere especial"))
    }
}
