package com.finance.presentation.controller

import com.finance.application.dto.account.AccountResponse
import com.finance.application.dto.group.GroupUserSummary
import com.finance.application.use_case.account.CreateAccountUseCase
import com.finance.application.use_case.account.DeleteAccountUseCase
import com.finance.application.use_case.account.GetTotalBalanceUseCase
import com.finance.application.use_case.account.LinkAccountToGroupUseCase
import com.finance.application.use_case.account.ListAccessibleAccountsUseCase
import com.finance.application.use_case.account.ListMyAccountsUseCase
import com.finance.application.use_case.account.UnlinkAccountFromGroupUseCase
import com.finance.application.use_case.account.UpdateAccountUseCase
import com.finance.domain.entity.UserEntity
import com.finance.domain.enum.AccountType
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import com.finance.presentation.exception.GlobalExceptionHandler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime
import java.util.UUID

class AccountControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var listAccessibleAccountsUseCase: ListAccessibleAccountsUseCase
    private lateinit var authUser: UserEntity

    @BeforeEach
    fun setup() {
        listAccessibleAccountsUseCase = mock(ListAccessibleAccountsUseCase::class.java)

        val controller = AccountController(
            createAccountUseCase = mock(CreateAccountUseCase::class.java),
            updateAccountUseCase = mock(UpdateAccountUseCase::class.java),
            listMyAccountsUseCase = mock(ListMyAccountsUseCase::class.java),
            listAccessibleAccountsUseCase = listAccessibleAccountsUseCase,
            getTotalBalanceUseCase = mock(GetTotalBalanceUseCase::class.java),
            linkAccountToGroupUseCase = mock(LinkAccountToGroupUseCase::class.java),
            unlinkAccountFromGroupUseCase = mock(UnlinkAccountFromGroupUseCase::class.java),
            deleteAccountUseCase = mock(DeleteAccountUseCase::class.java)
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
    fun `GET accessible should return 200 OK with individual and joint accounts`() {
        val now = LocalDateTime.now()
        val groupId = UUID.randomUUID()
        val jointAccId = UUID.randomUUID()
        val ownAccId = UUID.randomUUID()
        val partnerId = UUID.randomUUID()

        `when`(listAccessibleAccountsUseCase.execute(authUser.id)).thenReturn(
            listOf(
                AccountResponse(
                    id = ownAccId,
                    ownerId = authUser.id,
                    name = "Conta Pessoal",
                    startBalance = 10000,
                    balance = 10000,
                    type = AccountType.CHECKING,
                    tax = null,
                    groupId = null,
                    includeInTotal = true,
                    createdAt = now,
                    updatedAt = now,
                    isJoint = false,
                    allowedMembers = emptyList()
                ),
                AccountResponse(
                    id = jointAccId,
                    ownerId = null,
                    name = "Conta Conjunta Casa",
                    startBalance = 50000,
                    balance = 45000,
                    type = AccountType.CHECKING,
                    tax = null,
                    groupId = groupId,
                    includeInTotal = true,
                    createdAt = now,
                    updatedAt = now,
                    isJoint = true,
                    allowedMembers = listOf(
                        GroupUserSummary(authUser.id, "Arthur", "Menezes", "arthur@finance.com"),
                        GroupUserSummary(partnerId, "Maria", "Silva", "maria@finance.com")
                    )
                )
            )
        )

        mockMvc.get("/api/v1/accounts/accessible")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(2) }
                jsonPath("$[0].id") { value(ownAccId.toString()) }
                jsonPath("$[0].isJoint") { value(false) }
                jsonPath("$[1].id") { value(jointAccId.toString()) }
                jsonPath("$[1].isJoint") { value(true) }
                jsonPath("$[1].ownerId") { doesNotExist() }
                jsonPath("$[1].allowedMembers.length()") { value(2) }
            }
    }
}
