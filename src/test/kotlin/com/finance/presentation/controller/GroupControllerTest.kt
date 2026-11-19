package com.finance.presentation.controller

import com.finance.application.dto.account.CreateJointAccountRequest
import com.finance.application.dto.account.GroupAccountResponse
import com.finance.application.dto.account.JointAccountMemberShareResponse
import com.finance.application.dto.account.JointAccountSummaryResponse
import com.finance.application.dto.account.UpdateJointAccountRequest
import com.finance.application.dto.group.DebtSettlementResponse
import com.finance.application.dto.group.ForgiveDebtRequest
import com.finance.application.dto.group.GroupUserSummary
import com.finance.domain.enum.AccountType
import com.finance.domain.exception.AccessDeniedException
import com.finance.application.service.CreditCardService
import com.finance.application.use_case.account.AcceptAccountGroupRequestUseCase
import com.finance.application.use_case.account.CreateJointAccountUseCase
import com.finance.application.use_case.account.DeleteJointAccountUseCase
import com.finance.application.use_case.account.GetJointAccountSummaryUseCase
import com.finance.application.use_case.account.ListGroupAccountsUseCase
import com.finance.application.use_case.account.ListGroupJointAccountsUseCase
import com.finance.application.use_case.account.ListPendingAccountGroupRequestsUseCase
import com.finance.application.use_case.account.RejectAccountGroupRequestUseCase
import com.finance.application.use_case.account.UpdateJointAccountUseCase
import com.finance.application.use_case.group.AcceptGroupInvitationUseCase
import com.finance.application.use_case.group.CalculateGroupDebtsUseCase
import com.finance.application.use_case.group.CreateGroupUseCase
import com.finance.application.use_case.group.DeleteGroupUseCase
import com.finance.application.use_case.group.ForgiveGroupDebtUseCase
import com.finance.application.use_case.group.GetGroupUseCase
import com.finance.application.use_case.group.GetGroupUsedCategoriesUseCase
import com.finance.application.use_case.group.InviteMemberUseCase
import com.finance.application.use_case.group.LeaveGroupUseCase
import com.finance.application.use_case.group.ListGroupMembersUseCase
import com.finance.application.use_case.group.ListMyGroupInvitationsUseCase
import com.finance.application.use_case.group.ListMyGroupsUseCase
import com.finance.application.use_case.group.RejectGroupInvitationUseCase
import com.finance.application.use_case.group.RemoveMemberUseCase
import com.finance.application.use_case.group.RenameGroupUseCase
import com.finance.application.use_case.group.SettleGroupDebtUseCase
import com.finance.application.use_case.group.TransferOwnershipUseCase
import com.finance.application.use_case.group.UpdateMemberRoleUseCase
import com.finance.domain.entity.UserEntity
import com.finance.domain.exception.InvalidTransactionException
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import com.finance.presentation.exception.GlobalExceptionHandler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.`when`
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
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class GroupControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var forgiveGroupDebtUseCase: ForgiveGroupDebtUseCase
    private lateinit var createJointAccountUseCase: CreateJointAccountUseCase
    private lateinit var listGroupJointAccountsUseCase: ListGroupJointAccountsUseCase
    private lateinit var updateJointAccountUseCase: UpdateJointAccountUseCase
    private lateinit var deleteJointAccountUseCase: DeleteJointAccountUseCase
    private lateinit var getJointAccountSummaryUseCase: GetJointAccountSummaryUseCase
    private lateinit var authUser: UserEntity

    @BeforeEach
    fun setup() {
        forgiveGroupDebtUseCase = mock(ForgiveGroupDebtUseCase::class.java)
        createJointAccountUseCase = mock(CreateJointAccountUseCase::class.java)
        listGroupJointAccountsUseCase = mock(ListGroupJointAccountsUseCase::class.java)
        updateJointAccountUseCase = mock(UpdateJointAccountUseCase::class.java)
        deleteJointAccountUseCase = mock(DeleteJointAccountUseCase::class.java)
        getJointAccountSummaryUseCase = mock(GetJointAccountSummaryUseCase::class.java)

        val controller = GroupController(
            createGroupUseCase = mock(CreateGroupUseCase::class.java),
            deleteGroupUseCase = mock(DeleteGroupUseCase::class.java),
            renameGroupUseCase = mock(RenameGroupUseCase::class.java),
            getGroupUseCase = mock(GetGroupUseCase::class.java),
            listMyGroupsUseCase = mock(ListMyGroupsUseCase::class.java),
            inviteMemberUseCase = mock(InviteMemberUseCase::class.java),
            acceptGroupInvitationUseCase = mock(AcceptGroupInvitationUseCase::class.java),
            rejectGroupInvitationUseCase = mock(RejectGroupInvitationUseCase::class.java),
            listMyGroupInvitationsUseCase = mock(ListMyGroupInvitationsUseCase::class.java),
            removeMemberUseCase = mock(RemoveMemberUseCase::class.java),
            updateMemberRoleUseCase = mock(UpdateMemberRoleUseCase::class.java),
            transferOwnershipUseCase = mock(TransferOwnershipUseCase::class.java),
            leaveGroupUseCase = mock(LeaveGroupUseCase::class.java),
            listGroupMembersUseCase = mock(ListGroupMembersUseCase::class.java),
            listGroupAccountsUseCase = mock(ListGroupAccountsUseCase::class.java),
            listPendingAccountGroupRequestsUseCase = mock(ListPendingAccountGroupRequestsUseCase::class.java),
            acceptAccountGroupRequestUseCase = mock(AcceptAccountGroupRequestUseCase::class.java),
            rejectAccountGroupRequestUseCase = mock(RejectAccountGroupRequestUseCase::class.java),
            creditCardService = mock(CreditCardService::class.java),
            getGroupUsedCategoriesUseCase = mock(GetGroupUsedCategoriesUseCase::class.java),
            calculateGroupDebtsUseCase = mock(CalculateGroupDebtsUseCase::class.java),
            settleGroupDebtUseCase = mock(SettleGroupDebtUseCase::class.java),
            forgiveGroupDebtUseCase = forgiveGroupDebtUseCase,
            createJointAccountUseCase = createJointAccountUseCase,
            listGroupJointAccountsUseCase = listGroupJointAccountsUseCase,
            updateJointAccountUseCase = updateJointAccountUseCase,
            deleteJointAccountUseCase = deleteJointAccountUseCase,
            getJointAccountSummaryUseCase = getJointAccountSummaryUseCase
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
    fun `POST debts forgive should return 201 Created with isForgiveness true`() {
        val groupId = UUID.randomUUID()
        val debtorId = UUID.randomUUID()
        val settlementId = UUID.randomUUID()
        val request = ForgiveDebtRequest(
            debtorId = debtorId,
            amount = 5000,
            targetMonth = 11,
            targetYear = 2026,
            notes = "Perdoado"
        )
        val expected = DebtSettlementResponse(
            id = settlementId,
            groupId = groupId,
            debtorId = debtorId,
            debtor = GroupUserSummary(debtorId, "Maria", "Silva", "maria@finance.com"),
            creditorId = authUser.id,
            creditor = GroupUserSummary(authUser.id, "Arthur", "Menezes", "arthur@finance.com"),
            amount = 5000,
            targetMonth = 11,
            targetYear = 2026,
            date = LocalDate.of(2026, 11, 18),
            transactionId = null,
            notes = "Perdoado",
            isForgiveness = true,
            createdAt = LocalDateTime.now()
        )
        `when`(forgiveGroupDebtUseCase.execute(groupId, authUser.id, request)).thenReturn(expected)

        mockMvc.post("/api/v1/groups/$groupId/debts/forgive") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "debtorId": "$debtorId",
                  "amount": 5000,
                  "targetMonth": 11,
                  "targetYear": 2026,
                  "notes": "Perdoado"
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { value(settlementId.toString()) }
            jsonPath("$.amount") { value(5000) }
            jsonPath("$.isForgiveness") { value(true) }
            jsonPath("$.transactionId") { doesNotExist() }
        }
    }

    @Test
    fun `POST debts forgive should return 422 when InvalidTransactionException is thrown`() {
        val groupId = UUID.randomUUID()
        val debtorId = UUID.randomUUID()
        val request = ForgiveDebtRequest(
            debtorId = debtorId,
            amount = 99999,
            targetMonth = 11,
            targetYear = 2026,
            notes = null
        )
        `when`(forgiveGroupDebtUseCase.execute(groupId, authUser.id, request))
            .thenThrow(InvalidTransactionException("O valor do perdão não pode exceder o saldo devedor pendente."))

        mockMvc.post("/api/v1/groups/$groupId/debts/forgive") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "debtorId": "$debtorId",
                  "amount": 99999,
                  "targetMonth": 11,
                  "targetYear": 2026
                }
            """.trimIndent()
        }.andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.status") { value(422) }
        }
    }

    @Test
    fun `POST joint-accounts should return 201 Created`() {
        val groupId = UUID.randomUUID()
        val accountId = UUID.randomUUID()
        val partnerId = UUID.randomUUID()
        val request = CreateJointAccountRequest(
            name = "Conta Casa",
            startBalance = 20000,
            type = AccountType.CHECKING,
            tax = null,
            includeInTotal = true,
            memberIds = listOf(authUser.id, partnerId)
        )
        val expected = GroupAccountResponse(
            id = accountId,
            name = "Conta Casa",
            startBalance = 20000,
            balance = 20000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = groupId,
            includeInTotal = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            owner = null,
            isJoint = true,
            allowedMembers = listOf(
                GroupUserSummary(authUser.id, "Arthur", "Menezes", "arthur@finance.com"),
                GroupUserSummary(partnerId, "Maria", "Silva", "maria@finance.com")
            )
        )
        `when`(createJointAccountUseCase.execute(groupId, request, authUser.id)).thenReturn(expected)

        mockMvc.post("/api/v1/groups/$groupId/joint-accounts") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "name": "Conta Casa",
                  "startBalance": 20000,
                  "type": "CHECKING",
                  "includeInTotal": true,
                  "memberIds": ["${authUser.id}", "$partnerId"]
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { value(accountId.toString()) }
            jsonPath("$.isJoint") { value(true) }
            jsonPath("$.owner") { doesNotExist() }
            jsonPath("$.allowedMembers.length()") { value(2) }
        }
    }

    @Test
    fun `GET joint-accounts should return 200 OK or 403 Forbidden for non-admin`() {
        val groupId = UUID.randomUUID()
        val accountId = UUID.randomUUID()
        `when`(listGroupJointAccountsUseCase.execute(groupId, authUser.id)).thenReturn(
            listOf(
                GroupAccountResponse(
                    id = accountId,
                    name = "Fundo Viagem",
                    startBalance = 10000,
                    balance = 10000,
                    type = AccountType.SAVINGS,
                    tax = 10,
                    groupId = groupId,
                    includeInTotal = false,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                    owner = null,
                    isJoint = true,
                    allowedMembers = emptyList()
                )
            )
        )

        mockMvc.get("/api/v1/groups/$groupId/joint-accounts")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(1) }
                jsonPath("$[0].isJoint") { value(true) }
            }

        `when`(listGroupJointAccountsUseCase.execute(groupId, authUser.id))
            .thenThrow(AccessDeniedException("Apenas o dono ou administradores podem realizar esta ação."))

        mockMvc.get("/api/v1/groups/$groupId/joint-accounts")
            .andExpect {
                status { isForbidden() }
            }
    }

    @Test
    fun `PUT joint-accounts should return 200 OK`() {
        val groupId = UUID.randomUUID()
        val accountId = UUID.randomUUID()
        val request = UpdateJointAccountRequest(
            name = "Conta Casa Editada",
            startBalance = 20000,
            balance = 35000,
            type = AccountType.CHECKING,
            tax = null,
            includeInTotal = true,
            memberIds = listOf(authUser.id)
        )
        val expected = GroupAccountResponse(
            id = accountId,
            name = "Conta Casa Editada",
            startBalance = 20000,
            balance = 35000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = groupId,
            includeInTotal = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            owner = null,
            isJoint = true,
            allowedMembers = listOf(GroupUserSummary(authUser.id, "Arthur", "Menezes", "arthur@finance.com"))
        )
        `when`(updateJointAccountUseCase.execute(groupId, accountId, request, authUser.id)).thenReturn(expected)

        mockMvc.put("/api/v1/groups/$groupId/joint-accounts/$accountId") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "name": "Conta Casa Editada",
                  "startBalance": 20000,
                  "balance": 35000,
                  "type": "CHECKING",
                  "includeInTotal": true,
                  "memberIds": ["${authUser.id}"]
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("Conta Casa Editada") }
            jsonPath("$.balance") { value(35000) }
        }
    }

    @Test
    fun `DELETE joint-accounts should return 204 No Content`() {
        val groupId = UUID.randomUUID()
        val accountId = UUID.randomUUID()
        doNothing().`when`(deleteJointAccountUseCase).execute(groupId, accountId, authUser.id)

        mockMvc.delete("/api/v1/groups/$groupId/joint-accounts/$accountId")
            .andExpect {
                status { isNoContent() }
            }
    }

    @Test
    fun `GET joint-accounts summary should return 200 OK`() {
        val groupId = UUID.randomUUID()
        val accountId = UUID.randomUUID()
        val summary = JointAccountSummaryResponse(
            accountId = accountId,
            accountName = "Conta Casa",
            groupId = groupId,
            currentBalance = 15000L,
            month = 11,
            year = 2026,
            totalExpenses = 10000L,
            completedExpenses = 8000L,
            pendingExpenses = 2000L,
            totalIncome = 0L,
            totalContributions = 15000L,
            memberCount = 1,
            memberShares = listOf(
                JointAccountMemberShareResponse(
                    user = GroupUserSummary(authUser.id, "Arthur", "Menezes", "arthur@finance.com"),
                    shareAmount = 10000L,
                    contributedAmount = 15000L,
                    launchedExpensesAmount = 10000L,
                    netBalance = 5000L
                )
            )
        )
        `when`(getJointAccountSummaryUseCase.execute(groupId, accountId, 11, 2026, authUser.id)).thenReturn(summary)

        mockMvc.get("/api/v1/groups/$groupId/joint-accounts/$accountId/summary?month=11&year=2026")
            .andExpect {
                status { isOk() }
                jsonPath("$.accountId") { value(accountId.toString()) }
                jsonPath("$.totalExpenses") { value(10000) }
                jsonPath("$.totalContributions") { value(15000) }
                jsonPath("$.memberShares[0].netBalance") { value(5000) }
            }
    }
}
