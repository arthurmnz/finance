package com.finance.presentation.controller

import com.finance.application.dto.transaction.CreateTransactionRequest
import com.finance.application.dto.transaction.TransactionResponse
import com.finance.application.dto.transaction.UpdateTransactionRequest
import com.finance.application.use_case.transaction.CreateTransactionUseCase
import com.finance.application.use_case.transaction.DeleteTransactionUseCase
import com.finance.application.use_case.transaction.ListAccountTransactionsUseCase
import com.finance.application.use_case.transaction.ListCategoryTransactionsUseCase
import com.finance.application.use_case.transaction.UpdateTransactionUseCase
import com.finance.application.use_case.transaction.ToggleTransactionStatusUseCase
import com.finance.domain.entity.UserEntity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Endpoints para gerenciamento de transações")
class TransactionController(
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val listAccountTransactionsUseCase: ListAccountTransactionsUseCase,
    private val listCategoryTransactionsUseCase: ListCategoryTransactionsUseCase,
    private val toggleTransactionStatusUseCase: ToggleTransactionStatusUseCase
) {

    @PostMapping
    @Operation(summary = "Criar transação")
    fun create(
        @RequestBody request: CreateTransactionRequest,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<List<TransactionResponse>> {
        val response = createTransactionUseCase.execute(authenticatedUser.id, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar transação")
    fun update(
        @PathVariable id: UUID,
        @RequestBody request: UpdateTransactionRequest,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<TransactionResponse> {
        val response = updateTransactionUseCase.execute(authenticatedUser.id, id, request)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar transação")
    fun delete(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "false") deleteAllFuture: Boolean,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<Void> {
        deleteTransactionUseCase.execute(authenticatedUser.id, id, deleteAllFuture)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Listar transações de uma conta")
    fun listAccountTransactions(
        @PathVariable accountId: UUID,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<List<TransactionResponse>> {
        val response = listAccountTransactionsUseCase.execute(accountId)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Listar transações de uma categoria")
    fun listCategoryTransactions(
        @PathVariable categoryId: UUID,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<List<TransactionResponse>> {
        val response = listCategoryTransactionsUseCase.execute(categoryId)
        return ResponseEntity.ok(response)
    }

    @PutMapping("/{id}/toggle-status")
    @Operation(summary = "Alternar status da transação entre PENDING e COMPLETED")
    fun toggleStatus(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<TransactionResponse> {
        val response = toggleTransactionStatusUseCase.execute(authenticatedUser.id, id)
        return ResponseEntity.ok(response)
    }
}
