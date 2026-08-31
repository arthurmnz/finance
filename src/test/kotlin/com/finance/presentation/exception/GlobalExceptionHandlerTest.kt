package com.finance.presentation.exception

import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.servlet.resource.NoResourceFoundException
import kotlin.test.assertEquals

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun `should return 404 for NoResourceFoundException`() {
        val ex = NoResourceFoundException(HttpMethod.GET, "rota-inexistente", "rota-inexistente")
        val response = handler.handleNoResourceFound(ex)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(404, response.body?.status)
        assertEquals("Rota ou recurso não encontrado.", response.body?.message)
    }
}
