package com.finance.domain.value_object

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValueObjectsTest {

    @Test
    fun `should create valid Name`() {
        val name = Name("Arthur")
        assertEquals("Arthur", name.value)
        assertEquals("Arthur", name.formatted)
        assertEquals("Arthur", name.toString())
    }

    @Test
    fun `should fail when Name is blank or too short`() {
        assertThrows<IllegalArgumentException> { Name("   ") }
        assertThrows<IllegalArgumentException> { Name("A") }
    }

    @Test
    fun `should create valid Email`() {
        val email = Email("user@example.com")
        assertEquals("user@example.com", email.value)
        assertEquals("user@example.com", email.normalized)
    }

    @Test
    fun `should normalize Email to lowercase`() {
        val email = Email("  USER@Example.COM  ")
        assertEquals("user@example.com", email.normalized)
        assertEquals("user@example.com", email.toString())
    }

    @Test
    fun `should fail when Email format is invalid`() {
        assertThrows<IllegalArgumentException> { Email("invalid-email") }
        assertThrows<IllegalArgumentException> { Email("@domain.com") }
        assertThrows<IllegalArgumentException> { Email("user@") }
    }

    @Test
    fun `should create valid PasswordHash and mask toString`() {
        val hash = PasswordHash("\$2a\$12\$e8G1...")
        assertEquals("\$2a\$12\$e8G1...", hash.value)
        assertEquals("[PROTECTED]", hash.toString())
    }

    @Test
    fun `should fail when PasswordHash is blank`() {
        assertThrows<IllegalArgumentException> { PasswordHash("   ") }
    }
}
