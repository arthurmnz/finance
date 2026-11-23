package com.finance.presentation.controller

import com.finance.application.dto.transaction.TransactionResponse
import com.finance.application.use_case.transaction.CreateTransactionUseCase
import com.finance.application.use_case.transaction.DeleteTransactionUseCase
import com.finance.application.use_case.transaction.ListAccountTransactionsUseCase
import com.finance.application.use_case.transaction.ListCardTransactionsUseCase
import com.finance.application.use_case.transaction.ListCategoryTransactionsUseCase
import com.finance.application.use_case.transaction.ListInstallmentGroupUseCase
import com.finance.application.use_case.transaction.ListResponsibleTransactionsUseCase
import com.finance.application.use_case.transaction.ToggleTransactionStatusUseCase
import com.finance.application.use_case.transaction.UpdateInstallmentGroupAmountsUseCase
import com.finance.application.use_case.transaction.UpdateTransactionUseCase
import com.finance.domain.entity.UserEntity
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.InvalidTransactionException
import com.finance.domain.exception.TransactionNotFoundException
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import com.finance.presentation.exception.GlobalExceptionHandler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TransactionControllerTest {

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(): T {
        Mockito.any<T>()
        return null as T
    }

    private lateinit var mockMvc: MockMvc
    private lateinit var listInstallmentGroupUseCase: ListInstallmentGroupUseCase
    private lateinit var updateInstallmentGroupAmountsUseCase: UpdateInstallmentGroupAmountsUseCase
    private lateinit var authUser: UserEntity

    @BeforeEach
    fun setup() {
        listInstallmentGroupUseCase = mock(ListInstallmentGroupUseCase::class.java)
        updateInstallmentGroupAmountsUseCase = mock(UpdateInstallmentGroupAmountsUseCase::class.java)

        val controller = TransactionController(
            createTransactionUseCase = mock(CreateTransactionUseCase::class.java),
            updateTransactionUseCase = mock(UpdateTransactionUseCase::class.java),
            deleteTransactionUseCase = mock(DeleteTransactionUseCase::class.java),
            listAccountTransactionsUseCase = mock(ListAccountTransactionsUseCase::class.java),
            listCategoryTransactionsUseCase = mock(ListCategoryTransactionsUseCase::class.java),
            toggleTransactionStatusUseCase = mock(ToggleTransactionStatusUseCase::class.java),
            listResponsibleTransactionsUseCase = mock(ListResponsibleTransactionsUseCase::class.java),
            listCardTransactionsUseCase = mock(ListCardTransactionsUseCase::class.java),
            listInstallmentGroupUseCase = listInstallmentGroupUseCase,
            updateInstallmentGroupAmountsUseCase = updateInstallmentGroupAmountsUseCase
        )

        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(GlobalExceptionHandler())
            .setCustomArgumentResolvers(AuthenticationPrincipalArgumentResolver())
            .build()

        authUser = UserEntity(
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur@finance.com"),
            passwordHash = PasswordHash("hash")
        )
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
            authUser,
            null,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `GET group should return 200, 403 and 404 properly`() {
        val groupId = UUID.randomUUID()
        val accId = UUID.randomUUID()
        val now = LocalDateTime.now()

        val responses = listOf(
            TransactionResponse(
                id = UUID.randomUUID(),
                title = "Compra 4x",
                amount = 5000,
                date = LocalDate.of(2026, 11, 5),
                responsibleId = authUser.id,
                accountId = accId,
                type = TransactionType.EXPENSE,
                destinationAccountId = null,
                categoryId = null,
                status = TransactionStatus.COMPLETED,
                recurrenceGroupId = groupId,
                recurrenceFrequency = null,
                recurrenceInterval = 1,
                isInfinite = false,
                installmentNumber = 1,
                totalInstallments = 2,
                createdAt = now,
                updatedAt = now
            )
        )

        `when`(listInstallmentGroupUseCase.execute(groupId, authUser.id)).thenReturn(responses)

        mockMvc.get("/api/v1/transactions/group/$groupId")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(1) }
                jsonPath("$[0].amount") { value(5000) }
                jsonPath("$[0].installmentNumber") { value(1) }
            }

        val missingGroupId = UUID.randomUUID()
        `when`(listInstallmentGroupUseCase.execute(missingGroupId, authUser.id))
            .thenThrow(TransactionNotFoundException("Grupo não encontrado"))

        mockMvc.get("/api/v1/transactions/group/$missingGroupId")
            .andExpect { status { isNotFound() } }

        val forbiddenGroupId = UUID.randomUUID()
        `when`(listInstallmentGroupUseCase.execute(forbiddenGroupId, authUser.id))
            .thenThrow(AccessDeniedException("Sem permissão"))

        mockMvc.get("/api/v1/transactions/group/$forbiddenGroupId")
            .andExpect { status { isForbidden() } }
    }

    @Test
    fun `PUT group installments should return 200 on success and 422 on invalid payload`() {
        val groupId = UUID.randomUUID()
        val txId = UUID.randomUUID()
        val accId = UUID.randomUUID()
        val now = LocalDateTime.now()

        `when`(updateInstallmentGroupAmountsUseCase.execute(anyNonNull(), anyNonNull(), anyNonNull()))
            .thenReturn(
                listOf(
                    TransactionResponse(
                        id = txId,
                        title = "Compra 4x",
                        amount = 6500,
                        date = LocalDate.of(2026, 11, 5),
                        responsibleId = authUser.id,
                        accountId = accId,
                        type = TransactionType.EXPENSE,
                        destinationAccountId = null,
                        categoryId = null,
                        status = TransactionStatus.COMPLETED,
                        recurrenceGroupId = groupId,
                        recurrenceFrequency = null,
                        recurrenceInterval = 1,
                        isInfinite = false,
                        installmentNumber = 1,
                        totalInstallments = 2,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            )

        mockMvc.put("/api/v1/transactions/group/$groupId/installments") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"items":[{"transactionId":"$txId","amount":6500}]}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].amount") { value(6500) }
        }

        `when`(updateInstallmentGroupAmountsUseCase.execute(anyNonNull(), anyNonNull(), anyNonNull()))
            .thenThrow(InvalidTransactionException("Valor inválido"))

        mockMvc.put("/api/v1/transactions/group/$groupId/installments") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"items":[]}"""
        }.andExpect {
            status { isUnprocessableContent() }
        }
    }

    @Test
    fun `should return sanitized 400 Bad Request for invalid UUID path param and invalid JSON enum`() {
        mockMvc.get("/api/v1/transactions/group/not-a-valid-uuid")
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.status") { value(400) }
                jsonPath("$.message") { value("Parâmetro da requisição inválido.") }
            }

        mockMvc.post("/api/v1/transactions") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"title":"Teste","amount":1000,"date":"2026-11-10","accountId":"${UUID.randomUUID()}","type":"EXPENSE","remainderStrategy":"INVALID_ENUM"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.status") { value(400) }
            jsonPath("$.message") { value("Corpo da requisição inválido ou malformado.") }
        }
    }
}
