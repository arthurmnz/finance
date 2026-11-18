package com.finance.presentation.controller

import com.finance.application.dto.category.CategoryParentSummary
import com.finance.application.dto.category.CategoryResponse
import com.finance.application.use_case.category.CreateCategoryUseCase
import com.finance.application.use_case.category.DeleteCategoryUseCase
import com.finance.application.use_case.category.ListGroupCategoriesUseCase
import com.finance.application.use_case.category.ListUserCategoriesUseCase
import com.finance.application.use_case.category.SeedDefaultCategoriesUseCase
import com.finance.application.use_case.category.UpdateCategoryUseCase
import com.finance.domain.entity.UserEntity
import com.finance.domain.enum.CategoryLevel
import com.finance.domain.enum.CategoryType
import com.finance.domain.exception.InvalidCategoryException
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import com.finance.presentation.exception.GlobalExceptionHandler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime
import java.util.UUID

class CategoryControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var createCategoryUseCase: CreateCategoryUseCase
    private lateinit var updateCategoryUseCase: UpdateCategoryUseCase
    private lateinit var deleteCategoryUseCase: DeleteCategoryUseCase
    private lateinit var listUserCategoriesUseCase: ListUserCategoriesUseCase
    private lateinit var listGroupCategoriesUseCase: ListGroupCategoriesUseCase
    private lateinit var seedDefaultCategoriesUseCase: SeedDefaultCategoriesUseCase
    private lateinit var authenticatedUser: UserEntity

    @BeforeEach
    fun setup() {
        createCategoryUseCase = mock(CreateCategoryUseCase::class.java)
        updateCategoryUseCase = mock(UpdateCategoryUseCase::class.java)
        deleteCategoryUseCase = mock(DeleteCategoryUseCase::class.java)
        listUserCategoriesUseCase = mock(ListUserCategoriesUseCase::class.java)
        listGroupCategoriesUseCase = mock(ListGroupCategoriesUseCase::class.java)
        seedDefaultCategoriesUseCase = mock(SeedDefaultCategoriesUseCase::class.java)

        val controller = CategoryController(
            createCategoryUseCase = createCategoryUseCase,
            updateCategoryUseCase = updateCategoryUseCase,
            deleteCategoryUseCase = deleteCategoryUseCase,
            listUserCategoriesUseCase = listUserCategoriesUseCase,
            listGroupCategoriesUseCase = listGroupCategoriesUseCase,
            seedDefaultCategoriesUseCase = seedDefaultCategoriesUseCase
        )

        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(GlobalExceptionHandler())
            .setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver())
            .build()

        authenticatedUser = UserEntity(
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur.cat@finance.com"),
            passwordHash = PasswordHash("hash")
        )
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
            authenticatedUser,
            null,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `GET me should forward optional level and parentId query params`() {
        val parentId = UUID.randomUUID()
        val now = LocalDateTime.now()
        val item = CategoryResponse(
            id = UUID.randomUUID(),
            name = "Aluguel",
            description = null,
            userId = authenticatedUser.id,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#7D39EB",
            icon = "🔑",
            level = CategoryLevel.SPECIFIC,
            parentId = parentId,
            parent = CategoryParentSummary(parentId, "Moradia", "#7D39EB", "🏠", CategoryType.EXPENSE),
            isDefault = true,
            createdAt = now,
            updatedAt = now
        )

        `when`(listUserCategoriesUseCase.execute(authenticatedUser.id, CategoryLevel.SPECIFIC, parentId))
            .thenReturn(listOf(item))

        mockMvc.get("/api/v1/categories/me") {
            param("level", "SPECIFIC")
            param("parentId", parentId.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].name") { value("Aluguel") }
            jsonPath("$[0].level") { value("SPECIFIC") }
            jsonPath("$[0].parent.name") { value("Moradia") }
            jsonPath("$[0].isDefault") { value(true) }
        }
    }

    @Test
    fun `POST seed-defaults endpoints should return 200 OK for user and group`() {
        val groupId = UUID.randomUUID()
        val now = LocalDateTime.now()
        val sample = CategoryResponse(
            id = UUID.randomUUID(),
            name = "Moradia",
            description = null,
            userId = authenticatedUser.id,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#7D39EB",
            icon = "🏠",
            level = CategoryLevel.GENERAL,
            parentId = null,
            parent = null,
            isDefault = true,
            createdAt = now,
            updatedAt = now
        )

        `when`(seedDefaultCategoriesUseCase.seedForUser(authenticatedUser.id)).thenReturn(listOf(sample))
        `when`(seedDefaultCategoriesUseCase.seedForGroup(groupId, authenticatedUser.id)).thenReturn(listOf(sample.copy(userId = null, groupId = groupId)))

        mockMvc.post("/api/v1/categories/me/seed-defaults") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].name") { value("Moradia") }
            jsonPath("$[0].isDefault") { value(true) }
        }

        mockMvc.post("/api/v1/categories/group/$groupId/seed-defaults") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].groupId") { value(groupId.toString()) }
        }
    }

    @Test
    fun `DELETE general category with children should return 422 Unprocessable Entity`() {
        val categoryId = UUID.randomUUID()
        doThrow(InvalidCategoryException("Não é possível excluir uma categoria geral que possui subcategorias."))
            .`when`(deleteCategoryUseCase).execute(authenticatedUser.id, categoryId)

        mockMvc.delete("/api/v1/categories/$categoryId")
            .andExpect {
                status { isEqualTo(422) }
                jsonPath("$.status") { value(422) }
            }
    }
}
