package com.finance.application.use_case.account

import com.finance.application.dto.account.CreateJointAccountRequest
import com.finance.application.dto.account.UpdateAccountRequest
import com.finance.application.dto.account.UpdateJointAccountRequest
import com.finance.application.mapper.AccountDtoMapper
import com.finance.application.service.AccountAccessService
import com.finance.application.service.GroupService
import com.finance.domain.entity.AccountEntity
import com.finance.domain.entity.GroupEntity
import com.finance.domain.entity.GroupMemberEntity
import com.finance.domain.entity.JointAccountMemberEntity
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.entity.UserEntity
import com.finance.domain.enum.AccountType
import com.finance.domain.enum.GroupRole
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.repository.AccountGroupRequestRepository
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
import com.finance.domain.repository.JointAccountMemberRepository
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.repository.UserRepository
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JointAccountUseCasesTest {

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(): T {
        Mockito.any<T>()
        return null as T
    }

    private fun <T> matchList(predicate: (List<T>) -> Boolean): List<T> {
        Mockito.argThat<List<T>> { it != null && predicate(it) }
        return emptyList()
    }

    private lateinit var groupRepository: GroupRepository
    private lateinit var groupMemberRepository: GroupMemberRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var jointAccountMemberRepository: JointAccountMemberRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var accountGroupRequestRepository: AccountGroupRequestRepository
    private lateinit var userRepository: UserRepository
    private lateinit var groupService: GroupService
    private lateinit var accountAccessService: AccountAccessService
    private lateinit var accountDtoMapper: AccountDtoMapper

    private lateinit var createJointAccountUseCase: CreateJointAccountUseCase
    private lateinit var listGroupJointAccountsUseCase: ListGroupJointAccountsUseCase
    private lateinit var updateJointAccountUseCase: UpdateJointAccountUseCase
    private lateinit var deleteJointAccountUseCase: DeleteJointAccountUseCase
    private lateinit var getJointAccountSummaryUseCase: GetJointAccountSummaryUseCase
    private lateinit var listGroupAccountsUseCase: ListGroupAccountsUseCase
    private lateinit var listAccessibleAccountsUseCase: ListAccessibleAccountsUseCase
    private lateinit var linkAccountToGroupUseCase: LinkAccountToGroupUseCase
    private lateinit var unlinkAccountFromGroupUseCase: UnlinkAccountFromGroupUseCase
    private lateinit var updateAccountUseCase: UpdateAccountUseCase
    private lateinit var deleteAccountUseCase: DeleteAccountUseCase

    private lateinit var owner: UserEntity
    private lateinit var admin: UserEntity
    private lateinit var memberA: UserEntity
    private lateinit var memberB: UserEntity
    private lateinit var group: GroupEntity

    @BeforeEach
    fun setup() {
        groupRepository = mock(GroupRepository::class.java)
        groupMemberRepository = mock(GroupMemberRepository::class.java)
        accountRepository = mock(AccountRepository::class.java)
        jointAccountMemberRepository = mock(JointAccountMemberRepository::class.java)
        transactionRepository = mock(TransactionRepository::class.java)
        accountGroupRequestRepository = mock(AccountGroupRequestRepository::class.java)
        userRepository = mock(UserRepository::class.java)

        groupService = GroupService(groupRepository, groupMemberRepository)
        accountAccessService = AccountAccessService(groupMemberRepository, jointAccountMemberRepository)
        accountDtoMapper = AccountDtoMapper()

        createJointAccountUseCase = CreateJointAccountUseCase(
            groupService, groupMemberRepository, accountRepository, jointAccountMemberRepository, userRepository, accountDtoMapper
        )
        listGroupJointAccountsUseCase = ListGroupJointAccountsUseCase(
            groupService, accountRepository, jointAccountMemberRepository, userRepository, accountDtoMapper
        )
        updateJointAccountUseCase = UpdateJointAccountUseCase(
            groupService, groupMemberRepository, accountRepository, jointAccountMemberRepository, userRepository, accountDtoMapper
        )
        deleteJointAccountUseCase = DeleteJointAccountUseCase(
            groupService, accountRepository, transactionRepository, jointAccountMemberRepository, accountGroupRequestRepository
        )
        getJointAccountSummaryUseCase = GetJointAccountSummaryUseCase(
            groupService, groupMemberRepository, accountRepository, jointAccountMemberRepository,
            transactionRepository, userRepository, accountAccessService, accountDtoMapper
        )
        listGroupAccountsUseCase = ListGroupAccountsUseCase(
            groupService, accountRepository, jointAccountMemberRepository, userRepository, accountDtoMapper
        )
        listAccessibleAccountsUseCase = ListAccessibleAccountsUseCase(
            accountRepository, groupMemberRepository, jointAccountMemberRepository, userRepository, accountDtoMapper
        )
        linkAccountToGroupUseCase = LinkAccountToGroupUseCase(
            accountRepository, groupService, accountGroupRequestRepository, accountDtoMapper, accountAccessService
        )
        unlinkAccountFromGroupUseCase = UnlinkAccountFromGroupUseCase(
            accountRepository, groupService, accountAccessService
        )
        updateAccountUseCase = UpdateAccountUseCase(
            accountRepository, accountDtoMapper, accountAccessService, jointAccountMemberRepository, userRepository
        )
        deleteAccountUseCase = DeleteAccountUseCase(
            accountRepository, transactionRepository, accountGroupRequestRepository, jointAccountMemberRepository, accountAccessService
        )

        owner = UserEntity(id = UUID.fromString("00000000-0000-0000-0000-000000000001"), firstName = Name("Arthur"), lastName = Name("Owner"), email = Email("owner@finance.com"), passwordHash = PasswordHash("h"))
        admin = UserEntity(id = UUID.fromString("00000000-0000-0000-0000-000000000002"), firstName = Name("Bia"), lastName = Name("Admin"), email = Email("admin@finance.com"), passwordHash = PasswordHash("h"))
        memberA = UserEntity(id = UUID.fromString("00000000-0000-0000-0000-000000000003"), firstName = Name("Carlos"), lastName = Name("Membro"), email = Email("carlos@finance.com"), passwordHash = PasswordHash("h"))
        memberB = UserEntity(id = UUID.fromString("00000000-0000-0000-0000-000000000004"), firstName = Name("Diana"), lastName = Name("Membro"), email = Email("diana@finance.com"), passwordHash = PasswordHash("h"))
        group = GroupEntity(id = UUID.randomUUID(), name = "Casa")

        `when`(groupRepository.findById(group.id)).thenReturn(group)
        `when`(groupMemberRepository.findByGroupIdAndUserId(group.id, owner.id))
            .thenReturn(GroupMemberEntity(groupId = group.id, userId = owner.id, role = GroupRole.OWNER))
        `when`(groupMemberRepository.findByGroupIdAndUserId(group.id, admin.id))
            .thenReturn(GroupMemberEntity(groupId = group.id, userId = admin.id, role = GroupRole.ADMIN))
        `when`(groupMemberRepository.findByGroupIdAndUserId(group.id, memberA.id))
            .thenReturn(GroupMemberEntity(groupId = group.id, userId = memberA.id, role = GroupRole.MEMBER))
        `when`(groupMemberRepository.findByGroupIdAndUserId(group.id, memberB.id))
            .thenReturn(GroupMemberEntity(groupId = group.id, userId = memberB.id, role = GroupRole.MEMBER))

        listOf(owner.id, admin.id, memberA.id, memberB.id).forEach { uid ->
            `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, uid)).thenReturn(true)
        }
    }

    @Test
    fun `createJoint should succeed for ADMIN, reject MEMBER, empty memberIds and non-group member`() {
        `when`(accountRepository.save(anyNonNull())).thenAnswer { it.getArgument<AccountEntity>(0) }
        `when`(jointAccountMemberRepository.saveAll(anyNonNull())).thenAnswer { it.getArgument<List<JointAccountMemberEntity>>(0) }
        `when`(userRepository.findAllByIdIn(listOf(memberA.id, memberB.id))).thenReturn(listOf(memberA, memberB))

        val created = createJointAccountUseCase.execute(
            group.id,
            CreateJointAccountRequest(
                name = "Despesas Casa",
                startBalance = 10000,
                type = AccountType.CHECKING,
                tax = null,
                includeInTotal = true,
                memberIds = listOf(memberA.id, memberB.id, memberA.id)
            ),
            admin.id
        )

        assertTrue(created.isJoint)
        assertNull(created.owner)
        assertEquals(group.id, created.groupId)
        assertEquals(2, created.allowedMembers.size)

        // MEMBER cannot create joint account
        assertThrows<AccessDeniedException> {
            createJointAccountUseCase.execute(
                group.id,
                CreateJointAccountRequest("X", 0, AccountType.CHECKING, null, true, listOf(memberA.id)),
                memberA.id
            )
        }

        // Empty memberIds rejected
        assertThrows<IllegalArgumentException> {
            createJointAccountUseCase.execute(
                group.id,
                CreateJointAccountRequest("X", 0, AccountType.CHECKING, null, true, emptyList()),
                owner.id
            )
        }

        // Non-group member rejected
        val outsiderId = UUID.randomUUID()
        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, outsiderId)).thenReturn(false)
        assertThrows<IllegalArgumentException> {
            createJointAccountUseCase.execute(
                group.id,
                CreateJointAccountRequest("X", 0, AccountType.CHECKING, null, true, listOf(memberA.id, outsiderId)),
                owner.id
            )
        }
    }

    @Test
    fun `listGroupJointAccounts should show all joint accounts to OWNER even when not in allowedMembers, while listGroupAccounts hides restricted ones`() {
        val restrictedJoint = AccountEntity.createJoint(group.id, "Restrita", 5000, AccountType.CHECKING)
        val personalOwnedByMemberA = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = memberA.id,
            name = "Conta Carlos",
            startBalance = 2000,
            balance = 2000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = group.id
        )

        val linkA = JointAccountMemberEntity(accountId = restrictedJoint.id, userId = memberA.id)
        val linkB = JointAccountMemberEntity(accountId = restrictedJoint.id, userId = memberB.id)

        `when`(accountRepository.findAllByGroupIdAndIsJointTrue(group.id)).thenReturn(listOf(restrictedJoint))
        `when`(accountRepository.findAllByGroupId(group.id)).thenReturn(listOf(personalOwnedByMemberA, restrictedJoint))
        `when`(jointAccountMemberRepository.findAllByAccountIdIn(listOf(restrictedJoint.id))).thenReturn(listOf(linkA, linkB))
        `when`(userRepository.findAllByIdIn(listOf(memberA.id, memberB.id))).thenReturn(listOf(memberA, memberB))

        // Admin view (/joint-accounts) sees restrictedJoint with allowedMembers
        val adminView = listGroupJointAccountsUseCase.execute(group.id, owner.id)
        assertEquals(1, adminView.size)
        assertEquals(restrictedJoint.id, adminView[0].id)
        assertEquals(2, adminView[0].allowedMembers.size)

        // Regular MEMBER rejected on /joint-accounts
        assertThrows<AccessDeniedException> {
            listGroupJointAccountsUseCase.execute(group.id, memberA.id)
        }

        // /groups/{id}/accounts for owner (NOT in restrictedJoint) sees only personalOwnedByMemberA
        `when`(userRepository.findAllByIdIn(listOf(memberA.id))).thenReturn(listOf(memberA))
        val ownerGroupAccounts = listGroupAccountsUseCase.execute(group.id, owner.id)
        assertEquals(1, ownerGroupAccounts.size)
        assertEquals(personalOwnedByMemberA.id, ownerGroupAccounts[0].id)

        // /groups/{id}/accounts for memberA (IS in restrictedJoint) sees both
        `when`(userRepository.findAllByIdIn(listOf(memberA.id, memberB.id))).thenReturn(listOf(memberA, memberB))
        val memberAGroupAccounts = listGroupAccountsUseCase.execute(group.id, memberA.id)
        assertEquals(2, memberAGroupAccounts.size)
        assertTrue(memberAGroupAccounts.any { it.id == restrictedJoint.id && it.isJoint })
    }

    @Test
    fun `updateJoint should sync members via diff and preserve transactional balance when balance is null`() {
        val joint = AccountEntity.createJoint(group.id, "Original", 10000, AccountType.CHECKING)
        joint.subtractBalance(2500) // balance = 7500

        `when`(accountRepository.findById(joint.id)).thenReturn(joint)
        `when`(accountRepository.save(anyNonNull())).thenAnswer { it.getArgument<AccountEntity>(0) }
        `when`(jointAccountMemberRepository.findAllByAccountId(joint.id)).thenReturn(
            listOf(
                JointAccountMemberEntity(accountId = joint.id, userId = owner.id),
                JointAccountMemberEntity(accountId = joint.id, userId = memberA.id)
            )
        )
        `when`(jointAccountMemberRepository.saveAll(anyNonNull())).thenAnswer { it.getArgument<List<JointAccountMemberEntity>>(0) }
        `when`(userRepository.findAllByIdIn(listOf(memberA.id, memberB.id))).thenReturn(listOf(memberA, memberB))

        val updated = updateJointAccountUseCase.execute(
            group.id,
            joint.id,
            UpdateJointAccountRequest(
                name = "Editada",
                startBalance = 10000,
                balance = null,
                type = AccountType.CHECKING,
                tax = null,
                includeInTotal = true,
                memberIds = listOf(memberA.id, memberB.id)
            ),
            admin.id
        )

        assertEquals("Editada", updated.name)
        assertEquals(7500, updated.balance)
        verify(jointAccountMemberRepository).deleteByAccountIdAndUserId(joint.id, owner.id)
        verify(jointAccountMemberRepository, never()).deleteByAccountIdAndUserId(joint.id, memberA.id)
        verify(jointAccountMemberRepository).saveAll(matchList { list -> list.size == 1 && list[0].userId == memberB.id })

        // Reject account from different group
        val otherGroupJoint = AccountEntity.createJoint(UUID.randomUUID(), "Outra", 0, AccountType.CHECKING)
        `when`(accountRepository.findById(otherGroupJoint.id)).thenReturn(otherGroupJoint)
        assertThrows<AccountNotFoundException> {
            updateJointAccountUseCase.execute(
                group.id,
                otherGroupJoint.id,
                UpdateJointAccountRequest("X", 0, null, AccountType.CHECKING, null, true, listOf(memberA.id)),
                owner.id
            )
        }
    }

    @Test
    fun `deleteJoint should reverse COMPLETED incoming transfers and cascade delete members, requests and transactions`() {
        val joint = AccountEntity.createJoint(group.id, "Conjunta", 0, AccountType.CHECKING)
        val sourceAcc = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = memberA.id,
            name = "Conta Carlos",
            startBalance = 50000,
            balance = 40000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = group.id
        )
        val completedContribution = TransactionEntity(
            title = "Aporte",
            amount = 10000,
            date = LocalDate.of(2026, 11, 10),
            responsibleId = memberA.id,
            accountId = sourceAcc.id,
            type = TransactionType.TRANSFER,
            destinationAccountId = joint.id,
            categoryId = null,
            status = TransactionStatus.COMPLETED
        )
        val pendingContribution = TransactionEntity(
            title = "Aporte Pendente",
            amount = 5000,
            date = LocalDate.of(2026, 11, 11),
            responsibleId = memberA.id,
            accountId = sourceAcc.id,
            type = TransactionType.TRANSFER,
            destinationAccountId = joint.id,
            categoryId = null,
            status = TransactionStatus.PENDING
        )
        val expenseTx = TransactionEntity(
            title = "Mercado",
            amount = 3000,
            date = LocalDate.of(2026, 11, 12),
            responsibleId = memberB.id,
            accountId = joint.id,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED
        )

        `when`(accountRepository.findById(joint.id)).thenReturn(joint)
        `when`(accountRepository.findById(sourceAcc.id)).thenReturn(sourceAcc)
        `when`(transactionRepository.findByDestinationAccountId(joint.id))
            .thenReturn(listOf(completedContribution, pendingContribution))
        `when`(transactionRepository.findByAccountId(joint.id)).thenReturn(listOf(expenseTx))

        deleteJointAccountUseCase.execute(group.id, joint.id, owner.id)

        assertEquals(50000, sourceAcc.balance)
        verify(accountRepository).save(sourceAcc)
        verify(transactionRepository).deleteAll(listOf(completedContribution.id, pendingContribution.id))
        verify(transactionRepository).deleteAll(listOf(expenseTx.id))
        verify(jointAccountMemberRepository).deleteAllByAccountId(joint.id)
        verify(accountGroupRequestRepository).deleteAllByAccountId(joint.id)
        verify(accountRepository).deleteById(joint.id)
    }

    @Test
    fun `getJointAccountSummary should distribute remainder cents deterministically and calculate contributions and netBalance`() {
        val joint = AccountEntity.createJoint(group.id, "Casa", 0, AccountType.CHECKING)
        joint.addBalance(2000)

        val u1 = owner   // id ...0001
        val u2 = memberA // id ...0003
        val u3 = memberB // id ...0004

        `when`(accountRepository.findById(joint.id)).thenReturn(joint)
        `when`(jointAccountMemberRepository.existsByAccountIdAndUserId(joint.id, u2.id)).thenReturn(true)
        `when`(jointAccountMemberRepository.existsByAccountIdAndUserId(joint.id, admin.id)).thenReturn(false)
        `when`(jointAccountMemberRepository.findAllByAccountId(joint.id)).thenReturn(
            listOf(
                JointAccountMemberEntity(accountId = joint.id, userId = u3.id),
                JointAccountMemberEntity(accountId = joint.id, userId = u1.id),
                JointAccountMemberEntity(accountId = joint.id, userId = u2.id)
            )
        )
        `when`(userRepository.findAllByIdIn(anyNonNull())).thenReturn(listOf(u3, u1, u2))

        val accU1 = AccountEntity(ownerId = u1.id, name = "U1", startBalance = 5000, type = AccountType.CHECKING, tax = null)
        val otherJoint = AccountEntity.createJoint(group.id, "Outro Fundo", 5000, AccountType.CHECKING)

        val exp1 = TransactionEntity(
            title = "Luz",
            amount = 700,
            date = LocalDate.of(2026, 11, 5),
            responsibleId = u1.id,
            accountId = joint.id,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED
        )
        val exp2 = TransactionEntity(
            title = "Água",
            amount = 300,
            date = LocalDate.of(2026, 11, 8),
            responsibleId = u2.id,
            accountId = joint.id,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.PENDING
        )
        val inc = TransactionEntity(
            title = "Rendimento",
            amount = 200,
            date = LocalDate.of(2026, 11, 9),
            responsibleId = u1.id,
            accountId = joint.id,
            type = TransactionType.INCOME,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED
        )
        val otherMonthExp = TransactionEntity(
            title = "Mês anterior",
            amount = 9999,
            date = LocalDate.of(2026, 10, 5),
            responsibleId = u3.id,
            accountId = joint.id,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED
        )

        val contribU1 = TransactionEntity(
            title = "Aporte U1",
            amount = 1000,
            date = LocalDate.of(2026, 11, 2),
            responsibleId = u2.id, // mesmo se lançado por u2 a partir da conta de u1 -> conta como aporte de u1
            accountId = accU1.id,
            type = TransactionType.TRANSFER,
            destinationAccountId = joint.id,
            categoryId = null,
            status = TransactionStatus.COMPLETED
        )
        val contribU2FromJoint = TransactionEntity(
            title = "Aporte U2 via conjunta",
            amount = 500,
            date = LocalDate.of(2026, 11, 3),
            responsibleId = u2.id, // sourceAccount.ownerId == null -> fallback para responsibleId (u2)
            accountId = otherJoint.id,
            type = TransactionType.TRANSFER,
            destinationAccountId = joint.id,
            categoryId = null,
            status = TransactionStatus.COMPLETED
        )
        val pendingContrib = TransactionEntity(
            title = "Aporte Pendente",
            amount = 4000,
            date = LocalDate.of(2026, 11, 4),
            responsibleId = u3.id,
            accountId = accU1.id,
            type = TransactionType.TRANSFER,
            destinationAccountId = joint.id,
            categoryId = null,
            status = TransactionStatus.PENDING
        )

        `when`(transactionRepository.findByAccountId(joint.id)).thenReturn(listOf(exp1, exp2, inc, otherMonthExp))
        `when`(transactionRepository.findByDestinationAccountId(joint.id))
            .thenReturn(listOf(contribU1, contribU2FromJoint, pendingContrib))
        `when`(accountRepository.findAllByIdIn(listOf(accU1.id, otherJoint.id))).thenReturn(listOf(accU1, otherJoint))

        // Admin NÃO membro da conta conjunta não pode consultar resumo
        assertThrows<AccessDeniedException> {
            getJointAccountSummaryUseCase.execute(group.id, joint.id, 11, 2026, admin.id)
        }

        val summary = getJointAccountSummaryUseCase.execute(group.id, joint.id, 11, 2026, u2.id)

        assertEquals(1000L, summary.totalExpenses)
        assertEquals(700L, summary.completedExpenses)
        assertEquals(300L, summary.pendingExpenses)
        assertEquals(200L, summary.totalIncome)
        assertEquals(1500L, summary.totalContributions)
        assertEquals(3, summary.memberCount)
        assertEquals(2000L, summary.currentBalance)

        // 1000 / 3 = 333 resto 1 -> u1 recebe 334, u2 recebe 333, u3 recebe 333
        assertEquals(u1.id, summary.memberShares[0].user.id)
        assertEquals(334L, summary.memberShares[0].shareAmount)
        assertEquals(1000L, summary.memberShares[0].contributedAmount)
        assertEquals(700L, summary.memberShares[0].launchedExpensesAmount)
        assertEquals(666L, summary.memberShares[0].netBalance)

        assertEquals(u2.id, summary.memberShares[1].user.id)
        assertEquals(333L, summary.memberShares[1].shareAmount)
        assertEquals(500L, summary.memberShares[1].contributedAmount)
        assertEquals(300L, summary.memberShares[1].launchedExpensesAmount)
        assertEquals(167L, summary.memberShares[1].netBalance)

        assertEquals(u3.id, summary.memberShares[2].user.id)
        assertEquals(333L, summary.memberShares[2].shareAmount)
        assertEquals(0L, summary.memberShares[2].contributedAmount)
        assertEquals(0L, summary.memberShares[2].launchedExpensesAmount)
        assertEquals(-333L, summary.memberShares[2].netBalance)

        assertEquals(summary.totalExpenses, summary.memberShares.sumOf { it.shareAmount })
    }

    @Test
    fun `linkToGroup and unlinkFromGroup should reject joint accounts, and UpdateAccountUseCase allows OWNER or ADMIN`() {
        val joint = AccountEntity.createJoint(group.id, "Conjunta", 5000, AccountType.CHECKING)
        `when`(accountRepository.findById(joint.id)).thenReturn(joint)
        `when`(accountRepository.save(anyNonNull())).thenAnswer { it.getArgument<AccountEntity>(0) }
        `when`(jointAccountMemberRepository.findAllByAccountId(joint.id)).thenReturn(emptyList())
        `when`(userRepository.findAllByIdIn(emptyList())).thenReturn(emptyList())

        // Admin gets 400 IllegalArgumentException trying to link/unlink joint account
        assertThrows<IllegalArgumentException> {
            linkAccountToGroupUseCase.execute(joint.id, group.id, owner.id)
        }
        assertThrows<IllegalArgumentException> {
            unlinkAccountFromGroupUseCase.execute(joint.id, admin.id)
        }

        // Non-admin MEMBER gets 403 AccessDeniedException
        assertThrows<AccessDeniedException> {
            linkAccountToGroupUseCase.execute(joint.id, group.id, memberA.id)
        }
        assertThrows<AccessDeniedException> {
            unlinkAccountFromGroupUseCase.execute(joint.id, memberA.id)
        }

        // UpdateAccountUseCase allows OWNER/ADMIN via requireManage, denies MEMBER even if in joint_account_members
        `when`(jointAccountMemberRepository.existsByAccountIdAndUserId(joint.id, memberA.id)).thenReturn(true)
        assertThrows<AccessDeniedException> {
            updateAccountUseCase.execute(joint.id, UpdateAccountRequest("X", 5000, AccountType.CHECKING), memberA.id)
        }
        val updatedResp = updateAccountUseCase.execute(joint.id, UpdateAccountRequest("Conjunta Renomeada", 5000, AccountType.CHECKING), admin.id)
        assertEquals("Conjunta Renomeada", updatedResp.name)
        assertTrue(updatedResp.isJoint)
    }

    @Test
    fun `listAccessibleAccounts should include own, group individual, and authorized joint accounts only`() {
        val otherGroupId = UUID.randomUUID()
        val ownAcc = AccountEntity(ownerId = memberA.id, name = "Minha", startBalance = 1000, type = AccountType.CHECKING, tax = null)
        val groupIndAcc = AccountEntity(
            id = UUID.randomUUID(), ownerId = owner.id, name = "Grupo Ind", startBalance = 2000, balance = 2000,
            type = AccountType.CHECKING, tax = null, groupId = group.id
        )
        val allowedJoint = AccountEntity.createJoint(group.id, "Conjunta Autorizada", 3000, AccountType.CHECKING)
        val forbiddenJoint = AccountEntity.createJoint(group.id, "Conjunta Restrita", 4000, AccountType.CHECKING)
        val staleOtherGroupJoint = AccountEntity.createJoint(otherGroupId, "Grupo Que Sai", 5000, AccountType.CHECKING)

        `when`(accountRepository.findAllByOwnerId(memberA.id)).thenReturn(listOf(ownAcc))
        `when`(groupMemberRepository.findAllByUserId(memberA.id)).thenReturn(
            listOf(GroupMemberEntity(groupId = group.id, userId = memberA.id, role = GroupRole.MEMBER))
        )
        `when`(accountRepository.findAllByGroupIdIn(setOf(group.id))).thenReturn(
            listOf(groupIndAcc, allowedJoint, forbiddenJoint)
        )
        `when`(jointAccountMemberRepository.findAllByUserId(memberA.id)).thenReturn(
            listOf(
                JointAccountMemberEntity(accountId = allowedJoint.id, userId = memberA.id),
                JointAccountMemberEntity(accountId = staleOtherGroupJoint.id, userId = memberA.id)
            )
        )
        `when`(accountRepository.findAllByIdIn(listOf(allowedJoint.id, staleOtherGroupJoint.id)))
            .thenReturn(listOf(allowedJoint, staleOtherGroupJoint))
        `when`(jointAccountMemberRepository.findAllByAccountIdIn(listOf(allowedJoint.id))).thenReturn(
            listOf(
                JointAccountMemberEntity(accountId = allowedJoint.id, userId = memberA.id),
                JointAccountMemberEntity(accountId = allowedJoint.id, userId = memberB.id)
            )
        )
        `when`(userRepository.findAllByIdIn(listOf(memberA.id, memberB.id))).thenReturn(listOf(memberA, memberB))

        val result = listAccessibleAccountsUseCase.execute(memberA.id)

        assertEquals(3, result.size)
        assertTrue(result.any { it.id == ownAcc.id && !it.isJoint })
        assertTrue(result.any { it.id == groupIndAcc.id && !it.isJoint })
        val jointDto = result.first { it.id == allowedJoint.id }
        assertTrue(jointDto.isJoint)
        assertNull(jointDto.ownerId)
        assertEquals(2, jointDto.allowedMembers.size)
        assertFalse(result.any { it.id == forbiddenJoint.id })
        assertFalse(result.any { it.id == staleOtherGroupJoint.id })
    }

    @Test
    fun `getJointAccountSummary should credit legacy incoming transfer to source account owner even when responsibleId differs`() {
        val joint = AccountEntity.createJoint(group.id, "Casa", 0, AccountType.CHECKING)
        joint.addBalance(8000)

        val accMemberA = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = memberA.id,
            name = "Conta Carlos",
            startBalance = 20000,
            balance = 12000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = group.id
        )

        // Registro legado: memberB estava logado ao lançar aporte usando conta individual de memberA
        val legacyTransfer = TransactionEntity(
            title = "Aporte legado",
            amount = 8000,
            date = LocalDate.of(2026, 11, 15),
            responsibleId = memberB.id,
            accountId = accMemberA.id,
            type = TransactionType.TRANSFER,
            destinationAccountId = joint.id,
            categoryId = null,
            status = TransactionStatus.COMPLETED
        )

        `when`(accountRepository.findById(joint.id)).thenReturn(joint)
        `when`(jointAccountMemberRepository.existsByAccountIdAndUserId(joint.id, memberA.id)).thenReturn(true)
        `when`(jointAccountMemberRepository.findAllByAccountId(joint.id)).thenReturn(
            listOf(
                JointAccountMemberEntity(accountId = joint.id, userId = memberA.id),
                JointAccountMemberEntity(accountId = joint.id, userId = memberB.id)
            )
        )
        `when`(userRepository.findAllByIdIn(listOf(memberA.id, memberB.id))).thenReturn(listOf(memberA, memberB))
        `when`(transactionRepository.findByAccountId(joint.id)).thenReturn(emptyList())
        `when`(transactionRepository.findByDestinationAccountId(joint.id)).thenReturn(listOf(legacyTransfer))
        `when`(accountRepository.findAllByIdIn(listOf(accMemberA.id))).thenReturn(listOf(accMemberA))

        val summary = getJointAccountSummaryUseCase.execute(group.id, joint.id, 11, 2026, memberA.id)

        assertEquals(8000L, summary.totalContributions)
        val shareA = summary.memberShares.first { it.user.id == memberA.id }
        val shareB = summary.memberShares.first { it.user.id == memberB.id }
        assertEquals(8000L, shareA.contributedAmount)
        assertEquals(8000L, shareA.netBalance)
        assertEquals(0L, shareB.contributedAmount)
        assertEquals(0L, shareB.netBalance)
    }

    @Test
    fun `requireTransferSourceAccess should allow own individual or accessible joint account and reject other member individual account`() {
        val ownAcc = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = memberA.id,
            name = "Conta Carlos",
            startBalance = 5000,
            balance = 5000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = group.id
        )
        val colleagueGroupAcc = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = memberB.id,
            name = "Conta Diana",
            startBalance = 5000,
            balance = 5000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = group.id
        )
        val jointAcc = AccountEntity.createJoint(group.id, "Conjunta", 5000, AccountType.CHECKING)

        `when`(jointAccountMemberRepository.existsByAccountIdAndUserId(jointAcc.id, memberA.id)).thenReturn(true)
        `when`(jointAccountMemberRepository.existsByAccountIdAndUserId(jointAcc.id, memberB.id)).thenReturn(false)

        // Própria conta individual -> permitido
        accountAccessService.requireTransferSourceAccess(ownAcc, memberA.id)

        // Conta conjunta com acesso -> permitido
        accountAccessService.requireTransferSourceAccess(jointAcc, memberA.id)

        // Conta individual de outro membro do mesmo grupo -> bloqueado
        assertThrows<AccessDeniedException> {
            accountAccessService.requireTransferSourceAccess(colleagueGroupAcc, memberA.id)
        }

        // Conta conjunta sem acesso -> bloqueado
        assertThrows<AccessDeniedException> {
            accountAccessService.requireTransferSourceAccess(jointAcc, memberB.id)
        }
    }
}
