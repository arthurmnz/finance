package com.finance.presentation.controller

import com.finance.application.dto.account.AccountGroupRequestResponse
import com.finance.application.dto.account.AccountResponse
import com.finance.application.dto.account.CreateAccountRequest
import com.finance.application.dto.account.TotalBalanceResponse
import com.finance.application.dto.account.UpdateAccountRequest
import com.finance.application.use_case.account.AcceptAccountGroupRequestUseCase
import com.finance.application.use_case.account.CreateAccountUseCase
import com.finance.application.use_case.account.GetTotalBalanceUseCase
import com.finance.application.use_case.account.LinkAccountToGroupUseCase
import com.finance.application.use_case.account.ListGroupAccountsUseCase
import com.finance.application.use_case.account.ListMyAccountsUseCase
import com.finance.application.use_case.account.ListPendingAccountGroupRequestsUseCase
import com.finance.application.use_case.account.RejectAccountGroupRequestUseCase
import com.finance.application.use_case.account.UnlinkAccountFromGroupUseCase
import com.finance.application.use_case.account.UpdateAccountUseCase
import com.finance.domain.entity.AccountGroupRequestEntity
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
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Endpoints para gerenciamento de contas")
class AccountController(
    private val createAccountUseCase: CreateAccountUseCase,
    private val updateAccountUseCase: UpdateAccountUseCase,
    private val listMyAccountsUseCase: ListMyAccountsUseCase,
    private val getTotalBalanceUseCase: GetTotalBalanceUseCase,
    private val linkAccountToGroupUseCase: LinkAccountToGroupUseCase,
    private val unlinkAccountFromGroupUseCase: UnlinkAccountFromGroupUseCase
) {

    @PostMapping
    @Operation(summary = "Criar conta")
    fun create(
        @RequestBody request: CreateAccountRequest,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<AccountResponse> {
        val response = createAccountUseCase.execute(request, authenticatedUser.id)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar conta")
    fun update(
        @PathVariable id: UUID,
        @RequestBody request: UpdateAccountRequest,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<AccountResponse> {
        val response = updateAccountUseCase.execute(id, request, authenticatedUser.id)
        return ResponseEntity.ok(response)
    }

    @GetMapping
    @Operation(summary = "Listar minhas contas")
    fun listMyAccounts(
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<List<AccountResponse>> {
        val response = listMyAccountsUseCase.execute(authenticatedUser.id)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/total-balance")
    @Operation(summary = "Balanço total do usuário", description = "Retorna a soma dos balanços de todas as contas, podendo ignorar algumas.")
    fun getTotalBalance(
        @RequestParam(required = false) ignoreIds: Set<UUID>?,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<TotalBalanceResponse> {
        val response = getTotalBalanceUseCase.execute(authenticatedUser.id, ignoreIds)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/{id}/group/{groupId}")
    @Operation(summary = "Adicionar conta ao grupo", description = "Se for OWNER/ADMIN do grupo, vincula imediatamente. Se for MEMBER, cria solicitação.")
    fun linkToGroup(
        @PathVariable id: UUID,
        @PathVariable groupId: UUID,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<Any> {
        val response = linkAccountToGroupUseCase.execute(id, groupId, authenticatedUser.id)
        if (response is AccountGroupRequestEntity) {
            // Retorna algo simples para indicar a pendência (poderia usar DTO, mas um map resolve para mensagens simples)
            return ResponseEntity.accepted().body(mapOf("message" to "Solicitação de vínculo pendente enviada.", "requestId" to response.id))
        }
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}/group")
    @Operation(summary = "Desvincular conta de grupo", description = "O dono da conta ou OWNER/ADMIN do grupo pode desvincular.")
    fun unlinkFromGroup(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<Void> {
        unlinkAccountFromGroupUseCase.execute(id, authenticatedUser.id)
        return ResponseEntity.noContent().build()
    }
}
