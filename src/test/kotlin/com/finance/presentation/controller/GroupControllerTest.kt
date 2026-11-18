package com.finance.presentation.controller

import com.finance.application.dto.group.DebtSettlementResponse
import com.finance.application.dto.group.ForgiveDebtRequest
import com.finance.application.dto.group.GroupUserSummary
import com.finance.application.service.CreditCardService
import com.finance.application.use_case.account.AcceptAccountGroupRequestUseCase
import com.finance.application.use_case.account.ListGroupAccountsUseCase
import com.finance.application.use_case.account.ListPendingAccountGroupRequestsUseCase
import com.finance.application.use_case.account.RejectAccountGroupRequestUseCase
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
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class GroupControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var forgiveGroupDebtUseCase: ForgiveGroupDebtUseCase
    private lateinit var authUser: UserEntity

    @BeforeEach
    fun setup() {
        forgiveGroupDebtUseCase = mock(ForgiveGroupDebtUseCase::class.java)

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
            forgiveGroupDebtUseCase = forgiveGroupDebtUseCase
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
}
