package com.finance.presentation.exception

import com.finance.domain.exception.CreditCardNotFoundException
import com.finance.domain.exception.InvalidCategoryException
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.http.MockHttpInputMessage
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.time.DateTimeException
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse

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

    @Test
    fun `should return 422 for InvalidCategoryException`() {
        val ex = InvalidCategoryException("Categoria inválida para esta operação.")
        val response = handler.handleInvalidCategory(ex)

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.statusCode)
        assertEquals(422, response.body?.status)
        assertEquals("Categoria inválida para esta operação.", response.body?.message)
    }

    @Test
    fun `should return 400 for DateTimeException`() {
        val ex = DateTimeException("Invalid value for MonthOfYear: 13")
        val response = handler.handleBadRequest(ex)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(400, response.body?.status)
    }

    @Test
    fun `should return 404 for CreditCardNotFoundException`() {
        val ex = CreditCardNotFoundException()
        val response = handler.handleAccountNotFound(ex)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(404, response.body?.status)
        assertEquals("Cartão de crédito não encontrado.", response.body?.message)
    }

    @Test
    fun `should return sanitized 400 for HttpMessageNotReadableException without leaking internal class names`() {
        val ex = HttpMessageNotReadableException(
            "JSON parse error: Cannot deserialize value of type `com.finance.domain.enum.InstallmentRemainderStrategy` from String \"BAD\"",
            MockHttpInputMessage(byteArrayOf())
        )
        val response = handler.handleBadRequest(ex)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(400, response.body?.status)
        assertEquals("Corpo da requisição inválido ou malformado.", response.body?.message)
        assertFalse(response.body?.message.orEmpty().contains("com.finance"))
    }

    @Test
    fun `should return sanitized 400 for MethodArgumentTypeMismatchException`() {
        val method = GlobalExceptionHandlerTest::class.java.getDeclaredMethod("dummyUuidEndpoint", UUID::class.java)
        val param = MethodParameter(method, 0)
        val ex = MethodArgumentTypeMismatchException(
            "not-a-uuid",
            UUID::class.java,
            "recurrenceGroupId",
            param,
            IllegalArgumentException("Invalid UUID string: not-a-uuid")
        )
        val response = handler.handleBadRequest(ex)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(400, response.body?.status)
        assertEquals("Parâmetro da requisição inválido.", response.body?.message)
        assertFalse(response.body?.message.orEmpty().contains("UUID"))
    }

    @Suppress("unused")
    private fun dummyUuidEndpoint(id: UUID) {
        // Método auxiliar para instanciar MethodParameter no teste unitário
    }
}
