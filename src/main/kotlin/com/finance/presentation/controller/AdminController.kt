package com.finance.presentation.controller

import com.finance.application.dto.admin.AdminMetricsResponse
import com.finance.application.dto.admin.AdminUserResponse
import com.finance.application.dto.user.UserResponse
import com.finance.application.use_case.admin.DeleteUserByAdminUseCase
import com.finance.application.use_case.admin.GetAdminMetricsUseCase
import com.finance.application.use_case.admin.ListAdminUsersUseCase
import com.finance.application.use_case.admin.ToggleUserStatusByAdminUseCase
import com.finance.domain.entity.UserEntity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Endpoints administrativos do sistema")
class AdminController(
    private val getAdminMetricsUseCase: GetAdminMetricsUseCase,
    private val listAdminUsersUseCase: ListAdminUsersUseCase,
    private val toggleUserStatusByAdminUseCase: ToggleUserStatusByAdminUseCase,
    private val deleteUserByAdminUseCase: DeleteUserByAdminUseCase,
) {

    @GetMapping("/metrics")
    @Operation(summary = "Obter métricas consolidadas do sistema")
    fun getMetrics(
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<AdminMetricsResponse> {
        val response = getAdminMetricsUseCase.execute()
        return ResponseEntity.ok(response)
    }

    @GetMapping("/users")
    @Operation(summary = "Listar todos os usuários cadastrados e estatísticas de uso")
    fun listUsers(
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<List<AdminUserResponse>> {
        val response = listAdminUsersUseCase.execute()
        return ResponseEntity.ok(response)
    }

    @PatchMapping("/users/{id}/toggle-status")
    @Operation(summary = "Alternar status ativo/bloqueado de um usuário")
    fun toggleUserStatus(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<UserResponse> {
        val response = toggleUserStatusByAdminUseCase.execute(authenticatedUser.id, id)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Excluir permanentemente um usuário e todos os seus dados")
    fun deleteUser(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<Void> {
        deleteUserByAdminUseCase.execute(authenticatedUser.id, id)
        return ResponseEntity.noContent().build()
    }
}
