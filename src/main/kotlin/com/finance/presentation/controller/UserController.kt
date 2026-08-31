package com.finance.presentation.controller

import com.finance.application.dto.user.UpdateUserRequest
import com.finance.application.dto.user.UserResponse
import com.finance.application.mapper.UserDtoMapper
import com.finance.application.service.UserService
import com.finance.application.use_case.user.DeleteUserUseCase
import com.finance.application.use_case.user.UpdateUserUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
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

    @GetMapping
    @Operation(summary = "Listar todos os usuários")
    fun getAll(): ResponseEntity<List<UserResponse>> {
        val users = userService.findAll()
        return ResponseEntity.ok(users.map { userDtoMapper.toUserResponse(it) })
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar dados do usuário")
    fun update(
        @PathVariable id: UUID,
        @RequestBody request: UpdateUserRequest,
        @RequestHeader(name = "X-User-Id", required = false) authenticatedUserId: UUID? = null,
    ): ResponseEntity<UserResponse> {
        val updated = updateUserUseCase.execute(id, request, authenticatedUserId)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir usuário")
    fun delete(
        @PathVariable id: UUID,
        @RequestHeader(name = "X-User-Id", required = false) authenticatedUserId: UUID? = null,
    ): ResponseEntity<Void> {
        deleteUserUseCase.execute(id, authenticatedUserId)
        return ResponseEntity.noContent().build()
    }
}
