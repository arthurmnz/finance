package com.finance.presentation.exception

import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.UserAlreadyExistsException
import com.finance.domain.exception.UserNotFoundException
import com.finance.presentation.dto.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(ex: UserNotFoundException): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            error = HttpStatus.NOT_FOUND.reasonPhrase,
            message = ex.message ?: "Usuário não encontrado."
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error)
    }

    @ExceptionHandler(NoResourceFoundException::class, NoHandlerFoundException::class)
    fun handleNoResourceFound(ex: Exception): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            error = HttpStatus.NOT_FOUND.reasonPhrase,
            message = "Rota ou recurso não encontrado."
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error)
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(ex: HttpRequestMethodNotSupportedException): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            status = HttpStatus.METHOD_NOT_ALLOWED.value(),
            error = HttpStatus.METHOD_NOT_ALLOWED.reasonPhrase,
            message = "Método HTTP '${ex.method}' não suportado para esta rota."
        )
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error)
    }

    @ExceptionHandler(UserAlreadyExistsException::class)
    fun handleUserAlreadyExists(ex: UserAlreadyExistsException): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            status = HttpStatus.CONFLICT.value(),
            error = HttpStatus.CONFLICT.reasonPhrase,
            message = ex.message ?: "Conflito de dados."
        )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            status = HttpStatus.FORBIDDEN.value(),
            error = HttpStatus.FORBIDDEN.reasonPhrase,
            message = ex.message ?: "Acesso negado."
        )
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error)
    }

    @ExceptionHandler(IllegalArgumentException::class, HttpMessageNotReadableException::class)
    fun handleBadRequest(ex: Exception): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = ex.message ?: "Requisição inválida."
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
            message = "Ocorreu um erro interno no servidor."
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
    }
}
