package com.finance.presentation.controller

import com.finance.application.dto.friendship.FriendshipResponse
import com.finance.application.dto.friendship.SendFriendRequestRequest
import com.finance.application.use_case.friendship.AcceptFriendRequestUseCase
import com.finance.application.use_case.friendship.BlockFriendshipUseCase
import com.finance.application.use_case.friendship.ListMyFriendshipsUseCase
import com.finance.application.use_case.friendship.ListPendingReceivedRequestsUseCase
import com.finance.application.use_case.friendship.ListPendingSentRequestsUseCase
import com.finance.application.use_case.friendship.RejectFriendRequestUseCase
import com.finance.application.use_case.friendship.SendFriendRequestUseCase
import com.finance.domain.entity.UserEntity
import com.finance.domain.enum.FriendshipStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/friendships")
@Tag(name = "Friendships", description = "Endpoints de gerenciamento de amizades")
class FriendshipController(
    private val sendFriendRequestUseCase: SendFriendRequestUseCase,
    private val acceptFriendRequestUseCase: AcceptFriendRequestUseCase,
    private val rejectFriendRequestUseCase: RejectFriendRequestUseCase,
    private val blockFriendshipUseCase: BlockFriendshipUseCase,
    private val listMyFriendshipsUseCase: ListMyFriendshipsUseCase,
    private val listPendingReceivedRequestsUseCase: ListPendingReceivedRequestsUseCase,
    private val listPendingSentRequestsUseCase: ListPendingSentRequestsUseCase,
) {

    @GetMapping
    @Operation(
        summary = "Listar minhas amizades",
        description = "Lista todas as relações de amizade do usuário autenticado. Filtre por status usando o parâmetro `?status=ACCEPTED|PENDING|REJECTED|BLOCKED`.",
    )
    fun listMyFriendships(
        @RequestParam(required = false) status: FriendshipStatus?,
        @AuthenticationPrincipal authenticatedUser: UserEntity,
    ): ResponseEntity<List<FriendshipResponse>> {
        val response = listMyFriendshipsUseCase.execute(authenticatedUser.id, status)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/pending/received")
    @Operation(
        summary = "Listar solicitações recebidas",
        description = "Lista todas as solicitações de amizade pendentes recebidas pelo usuário autenticado.",
    )
    fun listPendingReceived(
        @AuthenticationPrincipal authenticatedUser: UserEntity,
    ): ResponseEntity<List<FriendshipResponse>> {
        val response = listPendingReceivedRequestsUseCase.execute(authenticatedUser.id)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/pending/sent")
    @Operation(
        summary = "Listar solicitações enviadas",
        description = "Lista todas as solicitações de amizade pendentes enviadas pelo usuário autenticado.",
    )
    fun listPendingSent(
        @AuthenticationPrincipal authenticatedUser: UserEntity,
    ): ResponseEntity<List<FriendshipResponse>> {
        val response = listPendingSentRequestsUseCase.execute(authenticatedUser.id)
        return ResponseEntity.ok(response)
    }

    @PostMapping
    @Operation(summary = "Solicitar amizade", description = "Envia uma solicitação de amizade para outro usuário.")
    fun sendFriendRequest(
        @RequestBody request: SendFriendRequestRequest,
        @AuthenticationPrincipal authenticatedUser: UserEntity,
    ): ResponseEntity<FriendshipResponse> {
        val response = sendFriendRequestUseCase.execute(authenticatedUser.id, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "Aceitar amizade", description = "Aceita uma solicitação de amizade pendente.")
    fun acceptFriendRequest(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserEntity,
    ): ResponseEntity<FriendshipResponse> {
        val response = acceptFriendRequestUseCase.execute(id, authenticatedUser.id)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Rejeitar amizade", description = "Rejeita uma solicitação de amizade pendente.")
    fun rejectFriendRequest(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserEntity,
    ): ResponseEntity<FriendshipResponse> {
        val response = rejectFriendRequestUseCase.execute(id, authenticatedUser.id)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/{id}/block")
    @Operation(summary = "Bloquear amizade", description = "Bloqueia uma relação de amizade existente.")
    fun blockFriendship(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserEntity,
    ): ResponseEntity<FriendshipResponse> {
        val response = blockFriendshipUseCase.execute(id, authenticatedUser.id)
        return ResponseEntity.ok(response)
    }
}
