package com.finance.presentation.controller

import com.finance.application.dto.user.UpdateUserRequest
import com.finance.application.dto.user.UserResponse
import com.finance.application.mapper.UserDtoMapper
import com.finance.application.service.UserService
import com.finance.application.use_case.user.DeleteUserUseCase
import com.finance.application.use_case.user.UpdateUserUseCase
import com.finance.domain.entity.UserEntity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Endpoints de gerenciamento de usuários")
class UserController(
    private val updateUserUseCase: UpdateUserUseCase,
    private val deleteUserUseCase: DeleteUserUseCase,
    private val userService: UserService,
    private val userDtoMapper: UserDtoMapper,
) {
    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID")
    fun getById(@PathVariable id: UUID): ResponseEntity<UserResponse> {
        val user = userService.findById(id)
        return ResponseEntity.ok(userDtoMapper.toUserResponse(user))
    }

    @GetMapping("/me")
    @Operation(summary = "Lista o usuário logado")
    fun getAll(
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<UserResponse> {
        return ResponseEntity.ok(userDtoMapper.toUserResponse(authenticatedUser))
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar dados do usuário")
    fun update(
        @PathVariable id: UUID,
        @RequestBody request: UpdateUserRequest,
        @AuthenticationPrincipal authenticatedUser: UserEntity,
    ): ResponseEntity<UserResponse> {
        val updated = updateUserUseCase.execute(id, request, authenticatedUser.id)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir usuário")
    fun delete(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserEntity,
    ): ResponseEntity<Void> {
        deleteUserUseCase.execute(id, authenticatedUser.id)
        return ResponseEntity.noContent().build()
    }
}
