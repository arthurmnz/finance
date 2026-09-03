package com.finance.presentation.controller

import com.finance.application.dto.category.CategoryResponse
import com.finance.application.dto.category.CreateCategoryRequest
import com.finance.application.dto.category.UpdateCategoryRequest
import com.finance.application.use_case.category.CreateCategoryUseCase
import com.finance.application.use_case.category.DeleteCategoryUseCase
import com.finance.application.use_case.category.ListGroupCategoriesUseCase
import com.finance.application.use_case.category.ListUserCategoriesUseCase
import com.finance.application.use_case.category.UpdateCategoryUseCase
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
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Endpoints para gerenciamento de categorias")
class CategoryController(
    private val createCategoryUseCase: CreateCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    private val listUserCategoriesUseCase: ListUserCategoriesUseCase,
    private val listGroupCategoriesUseCase: ListGroupCategoriesUseCase
) {

    @PostMapping
    @Operation(summary = "Criar categoria")
    fun create(
        @RequestBody request: CreateCategoryRequest,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<CategoryResponse> {
        val response = createCategoryUseCase.execute(authenticatedUser.id, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar categoria")
    fun update(
        @PathVariable id: UUID,
        @RequestBody request: UpdateCategoryRequest,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<CategoryResponse> {
        val response = updateCategoryUseCase.execute(authenticatedUser.id, id, request)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar categoria")
    fun delete(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<Void> {
        deleteCategoryUseCase.execute(authenticatedUser.id, id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/me")
    @Operation(summary = "Listar minhas categorias")
    fun listMyCategories(
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<List<CategoryResponse>> {
        val response = listUserCategoriesUseCase.execute(authenticatedUser.id)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "Listar categorias do grupo")
    fun listGroupCategories(
        @PathVariable groupId: UUID,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<List<CategoryResponse>> {
        // Detailed permission check can be added inside Use Case
        val response = listGroupCategoriesUseCase.execute(groupId)
        return ResponseEntity.ok(response)
    }
}
