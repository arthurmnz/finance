package com.finance.presentation.controller

import com.finance.application.dto.group.CreateGroupRequest
import com.finance.application.dto.group.GroupInvitationResponse
import com.finance.application.dto.group.GroupMemberResponse
import com.finance.application.dto.group.GroupMembersResponse
import com.finance.application.dto.group.GroupResponse
import com.finance.application.dto.group.GroupSummaryResponse
import com.finance.application.dto.group.InviteMemberRequest
import com.finance.application.dto.group.RenameGroupRequest
import com.finance.application.dto.group.TransferOwnershipRequest
import com.finance.application.dto.group.UpdateMemberRoleRequest
import com.finance.application.dto.account.AccountResponse
import com.finance.application.dto.account.AccountGroupRequestResponse
import com.finance.application.dto.account.GroupAccountResponse
import com.finance.application.use_case.group.AcceptGroupInvitationUseCase
import com.finance.application.use_case.group.CreateGroupUseCase
import com.finance.application.use_case.group.DeleteGroupUseCase
import com.finance.application.use_case.group.GetGroupUseCase
import com.finance.application.use_case.group.InviteMemberUseCase
import com.finance.application.use_case.group.LeaveGroupUseCase
import com.finance.application.use_case.group.ListGroupMembersUseCase
import com.finance.application.use_case.group.ListMyGroupInvitationsUseCase
import com.finance.application.use_case.group.ListMyGroupsUseCase
import com.finance.application.use_case.group.RejectGroupInvitationUseCase
import com.finance.application.use_case.group.RemoveMemberUseCase
import com.finance.application.use_case.group.RenameGroupUseCase
import com.finance.application.use_case.group.TransferOwnershipUseCase
import com.finance.application.use_case.group.UpdateMemberRoleUseCase
import com.finance.application.use_case.account.ListGroupAccountsUseCase
import com.finance.application.use_case.account.ListPendingAccountGroupRequestsUseCase
import com.finance.application.use_case.account.AcceptAccountGroupRequestUseCase
import com.finance.application.use_case.account.RejectAccountGroupRequestUseCase
import com.finance.domain.entity.UserEntity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/groups")
@Tag(name = "Groups", description = "Endpoints de gerenciamento de grupos financeiros")
class GroupController(
    private val createGroupUseCase: CreateGroupUseCase,
    private val deleteGroupUseCase: DeleteGroupUseCase,
    private val renameGroupUseCase: RenameGroupUseCase,
    private val getGroupUseCase: GetGroupUseCase,
    private val listMyGroupsUseCase: ListMyGroupsUseCase,
    private val inviteMemberUseCase: InviteMemberUseCase,
    private val acceptGroupInvitationUseCase: AcceptGroupInvitationUseCase,
    private val rejectGroupInvitationUseCase: RejectGroupInvitationUseCase,
    private val listMyGroupInvitationsUseCase: ListMyGroupInvitationsUseCase,
    private val removeMemberUseCase: RemoveMemberUseCase,
    private val updateMemberRoleUseCase: UpdateMemberRoleUseCase,
    private val transferOwnershipUseCase: TransferOwnershipUseCase,
    private val leaveGroupUseCase: LeaveGroupUseCase,
    private val listGroupMembersUseCase: ListGroupMembersUseCase,
    private val listGroupAccountsUseCase: ListGroupAccountsUseCase,
    private val listPendingAccountGroupRequestsUseCase: ListPendingAccountGroupRequestsUseCase,
    private val acceptAccountGroupRequestUseCase: AcceptAccountGroupRequestUseCase,
    private val rejectAccountGroupRequestUseCase: RejectAccountGroupRequestUseCase
) {

    // ── Grupo ──────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Criar grupo", description = "Cria um novo grupo e adiciona o criador como OWNER.")
    fun create(
        @RequestBody request: CreateGroupRequest,
        @AuthenticationPrincipal authenticatedUser: UserEntity,
    ): ResponseEntity<GroupResponse> {
        val response = createGroupUseCase.execute(request, authenticatedUser.id)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping
    @Operation(summary = "Listar meus grupos", description = "Retorna todos os grupos em que o usuário é membro.")
    fun listMyGroups(
        @AuthenticationPrincipal authenticatedUser: UserEntity,
    ): ResponseEntity<List<GroupSummaryResponse>> {
        val response = listMyGroupsUseCase.execute(authenticatedUser.id)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar grupo", description = "Retorna os detalhes e membros de um grupo. Apenas membros podem acessar.")
    fun getGroup(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserEntity,
    ): ResponseEntity<GroupResponse> {
        val response = getGroupUseCase.execute(id, authenticatedUser.id)
        return ResponseEntity.ok(response)
    }

    @PutMapping("/{id}")
    @Operation(summary = "Renomear grupo", description = "Renomeia o grupo. OWNER e ADMIN podem executar.")
    fun rename(
        @PathVariable id: UUID,
        @RequestBody request: RenameGroupRequest,
        @AuthenticationPrincipal authenticatedUser: UserEntity,
    ): ResponseEntity<GroupResponse> {
        val response = renameGroupUseCase.execute(id, request, authenticatedUser.id)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir grupo", description = "Exclui o grupo e todos os seus dados. Apenas o OWNER pode executar.")
    fun delete(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserEntity,
    ): ResponseEntity<Void> {
        deleteGroupUseCase.execute(id, authenticatedUser.id)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{id}/leave")
    @Operation(summary = "Sair do grupo", description = "Sai do grupo. OWNER deve transferir a titularidade antes de sair.")
    fun leave(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserEntity,
    ): ResponseEntity<Void> {
        leaveGroupUseCase.execute(id, authenticatedUser.id)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{id}/transfer")
    @Operation(summary = "Transferir titularidade", description = "Transfere o OWNER para outro membro. O atual OWNER vira ADMIN.")
    fun transferOwnership(
        @PathVariable id: UUID,
        @RequestBody request: TransferOwnershipRequest,
        @AuthenticationPrincipal authenticatedUser: UserEntity,
    ): ResponseEntity<GroupResponse> {
        val response = transferOwnershipUseCase.execute(id, request, authenticatedUser.id)
        return ResponseEntity.ok(response)
    }

    // ── Membros ────────────────────────────────────────────────────────────

    @GetMapping("/{id}/members")
    @Operation(summary = "Listar membros", description = "Lista todos os membros do grupo com suas informações e cargos. Apenas membros podem acessar.")
    fun listMembers(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserEntity,
    ): ResponseEntity<GroupMembersResponse> {
        val response = listGroupMembersUseCase.execute(id, authenticatedUser.id)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}/members")
    @Operation(summary = "Remover membro", description = "Remove um membro do grupo pelo e-mail. OWNER e ADMIN podem executar.")
    fun removeMember(
        @PathVariable id: UUID,
        @RequestParam email: String,
        @AuthenticationPrincipal authenticatedUser: UserEntity,
    ): ResponseEntity<GroupResponse> {
        val response = removeMemberUseCase.execute(id, email, authenticatedUser.id)
        return ResponseEntity.ok(response)
    }

    @PatchMapping("/{id}/members/role")
    @Operation(summary = "Atualizar role de membro", description = "Altera o cargo de um membro (ADMIN/MEMBER). Apenas o OWNER pode executar.")
    fun updateMemberRole(
        @PathVariable id: UUID,
        @RequestBody request: UpdateMemberRoleRequest,
        @AuthenticationPrincipal authenticatedUser: UserEntity,
    ): ResponseEntity<GroupResponse> {
        val response = updateMemberRoleUseCase.execute(id, request, authenticatedUser.id)
        return ResponseEntity.ok(response)
    }

    // ── Convites ───────────────────────────────────────────────────────────

    @PostMapping("/{id}/invitations")
    @Operation(summary = "Convidar membro", description = "Envia convite para um amigo entrar no grupo. OWNER e ADMIN podem executar.")
    fun inviteMember(
        @PathVariable id: UUID,
        @RequestBody request: InviteMemberRequest,
        @AuthenticationPrincipal authenticatedUser: UserEntity,
    ): ResponseEntity<GroupInvitationResponse> {
        val response = inviteMemberUseCase.execute(id, request, authenticatedUser.id)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/invitations/pending")
    @Operation(summary = "Listar convites pendentes", description = "Lista todos os convites de grupo pendentes recebidos pelo usuário.")
    fun listPendingInvitations(
        @AuthenticationPrincipal authenticatedUser: UserEntity,
    ): ResponseEntity<List<GroupInvitationResponse>> {
        val response = listMyGroupInvitationsUseCase.execute(authenticatedUser.id)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/invitations/{invitationId}/accept")
    @Operation(summary = "Aceitar convite", description = "Aceita um convite de grupo pendente e entra como MEMBER.")
    fun acceptInvitation(
        @PathVariable invitationId: UUID,
        @AuthenticationPrincipal authenticatedUser: UserEntity,
    ): ResponseEntity<GroupResponse> {
        val response = acceptGroupInvitationUseCase.execute(invitationId, authenticatedUser.id)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/invitations/{invitationId}/reject")
    @Operation(summary = "Recusar convite", description = "Recusa um convite de grupo pendente.")
    fun rejectInvitation(
        @PathVariable invitationId: UUID,
        @AuthenticationPrincipal authenticatedUser: UserEntity,
    ): ResponseEntity<GroupInvitationResponse> {
        val response = rejectGroupInvitationUseCase.execute(invitationId, authenticatedUser.id)
        return ResponseEntity.ok(response)
    }

    // ── Contas de Grupo ──────────────────────────────────────────────────────────

    @GetMapping("/{id}/accounts")
    @Operation(summary = "Listar contas do grupo", description = "Lista todas as contas vinculadas a este grupo.")
    fun listAccounts(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<List<GroupAccountResponse>> {
        val response = listGroupAccountsUseCase.execute(id, authenticatedUser.id)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}/account-requests")
    @Operation(summary = "Listar solicitações pendentes", description = "Lista as solicitações de contas para entrar no grupo.")
    fun listAccountRequests(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<List<AccountGroupRequestResponse>> {
        val response = listPendingAccountGroupRequestsUseCase.execute(id, authenticatedUser.id)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/{id}/account-requests/{requestId}/accept")
    @Operation(summary = "Aceitar solicitação de conta")
    fun acceptAccountRequest(
        @PathVariable id: UUID,
        @PathVariable requestId: UUID,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<Void> {
        acceptAccountGroupRequestUseCase.execute(requestId, id, authenticatedUser.id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/account-requests/{requestId}/reject")
    @Operation(summary = "Recusar solicitação de conta")
    fun rejectAccountRequest(
        @PathVariable id: UUID,
        @PathVariable requestId: UUID,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<Void> {
        rejectAccountGroupRequestUseCase.execute(requestId, id, authenticatedUser.id)
        return ResponseEntity.noContent().build()
    }
}
