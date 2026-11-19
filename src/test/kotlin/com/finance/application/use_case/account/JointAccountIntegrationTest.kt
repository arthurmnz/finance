package com.finance.application.use_case.account

import com.finance.application.dto.account.CreateJointAccountRequest
import com.finance.application.dto.account.UpdateJointAccountRequest
import com.finance.application.dto.credit_card.CreateCreditCardRequest
import com.finance.application.dto.credit_card.PayInvoiceRequest
import com.finance.application.dto.credit_card.UpdateCreditCardRequest
import com.finance.application.dto.transaction.AccountSplitRequest
import com.finance.application.dto.transaction.CreateTransactionRequest
import com.finance.application.dto.transaction.UpdateTransactionRequest
import com.finance.application.service.CreditCardService
import com.finance.application.use_case.admin.DeleteUserByAdminUseCase
import com.finance.application.use_case.admin.ListAdminUsersUseCase
import com.finance.application.use_case.group.CalculateGroupDebtsUseCase
import com.finance.application.use_case.group.DeleteGroupUseCase
import com.finance.application.use_case.group.GetGroupUsedCategoriesUseCase
import com.finance.application.use_case.group.LeaveGroupUseCase
import com.finance.application.use_case.group.RemoveMemberUseCase
import com.finance.application.use_case.transaction.CreateTransactionUseCase
import com.finance.application.use_case.transaction.DeleteTransactionUseCase
import com.finance.application.use_case.transaction.ListAccountTransactionsUseCase
import com.finance.application.use_case.transaction.ListCategoryTransactionsUseCase
import com.finance.application.use_case.transaction.ToggleTransactionStatusUseCase
import com.finance.application.use_case.transaction.UpdateTransactionUseCase
import com.finance.application.use_case.user.DeleteUserUseCase
import com.finance.domain.entity.AccountEntity
import com.finance.domain.entity.CategoryEntity
import com.finance.domain.entity.CreditCardEntity
import com.finance.domain.entity.GroupEntity
import com.finance.domain.entity.GroupMemberEntity
import com.finance.domain.entity.UserEntity
import com.finance.domain.enum.AccountType
import com.finance.domain.enum.CategoryLevel
import com.finance.domain.enum.CategoryType
import com.finance.domain.enum.GroupRole
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.InvalidTransactionException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.CreditCardRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
import com.finance.domain.repository.JointAccountMemberRepository
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.repository.UserRepository
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import com.finance.infraestructure.seeder.JointAccountSchemaMigrationRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
@Transactional
class JointAccountIntegrationTest @Autowired constructor(
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val accountRepository: AccountRepository,
    private val jointAccountMemberRepository: JointAccountMemberRepository,
    private val categoryRepository: CategoryRepository,
    private val creditCardRepository: CreditCardRepository,
    private val transactionRepository: TransactionRepository,
    private val createJointAccountUseCase: CreateJointAccountUseCase,
    private val listGroupJointAccountsUseCase: ListGroupJointAccountsUseCase,
    private val listGroupAccountsUseCase: ListGroupAccountsUseCase,
    private val listAccessibleAccountsUseCase: ListAccessibleAccountsUseCase,
    private val listMyAccountsUseCase: ListMyAccountsUseCase,
    private val getTotalBalanceUseCase: GetTotalBalanceUseCase,
    private val updateJointAccountUseCase: UpdateJointAccountUseCase,
    private val deleteJointAccountUseCase: DeleteJointAccountUseCase,
    private val getJointAccountSummaryUseCase: GetJointAccountSummaryUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val toggleTransactionStatusUseCase: ToggleTransactionStatusUseCase,
    private val listAccountTransactionsUseCase: ListAccountTransactionsUseCase,
    private val listCategoryTransactionsUseCase: ListCategoryTransactionsUseCase,
    private val getGroupUsedCategoriesUseCase: GetGroupUsedCategoriesUseCase,
    private val calculateGroupDebtsUseCase: CalculateGroupDebtsUseCase,
    private val creditCardService: CreditCardService,
    private val removeMemberUseCase: RemoveMemberUseCase,
    private val leaveGroupUseCase: LeaveGroupUseCase,
    private val deleteGroupUseCase: DeleteGroupUseCase,
    private val deleteUserUseCase: DeleteUserUseCase,
    private val listAdminUsersUseCase: ListAdminUsersUseCase,
    private val deleteUserByAdminUseCase: DeleteUserByAdminUseCase,
    private val jointAccountSchemaMigrationRunner: JointAccountSchemaMigrationRunner
) {

    private fun createUser(prefix: String): UserEntity {
        return userRepository.save(
            UserEntity(
                firstName = Name(prefix),
                lastName = Name("Test"),
                email = Email("$prefix.${UUID.randomUUID()}@finance.com"),
                passwordHash = PasswordHash("hash123")
            )
        )
    }

    @Test
    fun `full joint account lifecycle with transactions, category isolation, debts, admin and cascades`() {
        // Runner should execute idempotently on H2 without failing
        jointAccountSchemaMigrationRunner.run()

        val owner = createUser("Owner")
        val alice = createUser("Alice")
        val bob = createUser("Bob")
        val carol = createUser("Carol")

        val group = groupRepository.save(GroupEntity(name = "Apartamento"))
        groupMemberRepository.save(GroupMemberEntity(groupId = group.id, userId = owner.id, role = GroupRole.OWNER))
        groupMemberRepository.save(GroupMemberEntity(groupId = group.id, userId = alice.id, role = GroupRole.ADMIN))
        groupMemberRepository.save(GroupMemberEntity(groupId = group.id, userId = bob.id, role = GroupRole.MEMBER))
        groupMemberRepository.save(GroupMemberEntity(groupId = group.id, userId = carol.id, role = GroupRole.MEMBER))

        val alicePersonal = accountRepository.save(
            AccountEntity(ownerId = alice.id, name = "Conta Alice", startBalance = 100000, type = AccountType.CHECKING, tax = null)
        )
        val bobPersonal = accountRepository.save(
            AccountEntity(ownerId = bob.id, name = "Conta Bob", startBalance = 80000, type = AccountType.CHECKING, tax = null)
        )

        // Owner cria conta conjunta apenas para Alice e Bob (Owner e Carol NÃO têm acesso à movimentação)
        val jointResp = createJointAccountUseCase.execute(
            group.id,
            CreateJointAccountRequest(
                name = "Conta Conjunta AB",
                startBalance = 10000,
                type = AccountType.CHECKING,
                tax = null,
                includeInTotal = true,
                memberIds = listOf(alice.id, bob.id)
            ),
            owner.id
        )
        assertTrue(jointResp.isJoint)
        assertNull(jointResp.owner)
        assertEquals(2, jointResp.allowedMembers.size)

        // Owner vê em /joint-accounts, mas NÃO em /accounts, /total-balance, /groups/{id}/accounts ou /accessible
        assertEquals(1, listGroupJointAccountsUseCase.execute(group.id, owner.id).size)
        assertTrue(listGroupAccountsUseCase.execute(group.id, owner.id).isEmpty())
        assertTrue(listAccessibleAccountsUseCase.execute(owner.id).isEmpty())
        assertTrue(listMyAccountsUseCase.execute(alice.id).none { it.id == jointResp.id })
        assertEquals(100000, getTotalBalanceUseCase.execute(alice.id, null).total)
        assertTrue(listAccessibleAccountsUseCase.execute(alice.id).any { it.id == jointResp.id })
        assertTrue(listGroupAccountsUseCase.execute(group.id, bob.id).any { it.id == jointResp.id })

        // Owner não autorizado não pode consultar extrato nem resumo
        assertThrows<AccessDeniedException> {
            listAccountTransactionsUseCase.execute(jointResp.id, null, null, owner.id)
        }
        assertThrows<AccessDeniedException> {
            getJointAccountSummaryUseCase.execute(group.id, jointResp.id, 11, 2026, owner.id)
        }

        // Categoria do grupo e categoria pessoal de Alice
        val groupCategory = categoryRepository.save(
            CategoryEntity(
                name = "Moradia Grupo",
                description = null,
                userId = null,
                groupId = group.id,
                type = CategoryType.EXPENSE,
                color = "#7D39EB",
                icon = "🏠",
                level = CategoryLevel.GENERAL
            )
        )
        val aliceSecretCategory = categoryRepository.save(
            CategoryEntity(
                name = "Segredo Alice",
                description = null,
                userId = alice.id,
                groupId = null,
                type = CategoryType.EXPENSE,
                color = "#111111",
                icon = "🔒",
                level = CategoryLevel.GENERAL
            )
        )

        // Alice realiza aporte (TRANSFER) de sua conta para a conta conjunta
        createTransactionUseCase.execute(
            alice.id,
            CreateTransactionRequest(
                title = "Aporte Alice",
                amount = 20000,
                date = LocalDate.of(2026, 11, 5),
                accountId = alicePersonal.id,
                type = TransactionType.TRANSFER,
                destinationAccountId = jointResp.id,
                status = TransactionStatus.COMPLETED
            )
        )
        // Transferência com generatesDebt = true envolvendo conta conjunta é rejeitada
        assertThrows<InvalidTransactionException> {
            createTransactionUseCase.execute(
                alice.id,
                CreateTransactionRequest(
                    title = "Tentativa Dívida",
                    amount = 5000,
                    date = LocalDate.of(2026, 11, 5),
                    accountId = alicePersonal.id,
                    type = TransactionType.TRANSFER,
                    destinationAccountId = jointResp.id,
                    status = TransactionStatus.COMPLETED,
                    generatesDebt = true
                )
            )
        }

        // Alice lança despesa na conta conjunta usando categoria pessoal dela e categoria do grupo
        val aliceExp = createTransactionUseCase.execute(
            alice.id,
            CreateTransactionRequest(
                title = "Compra Secreta",
                amount = 5001,
                date = LocalDate.of(2026, 11, 10),
                accountId = jointResp.id,
                type = TransactionType.EXPENSE,
                categoryId = aliceSecretCategory.id,
                status = TransactionStatus.COMPLETED
            )
        ).first()
        createTransactionUseCase.execute(
            bob.id,
            CreateTransactionRequest(
                title = "Condomínio",
                amount = 10000,
                date = LocalDate.of(2026, 11, 12),
                accountId = jointResp.id,
                type = TransactionType.EXPENSE,
                categoryId = groupCategory.id,
                status = TransactionStatus.PENDING
            )
        )

        // Rateio com split misto (conta pessoal de Bob + conta conjunta)
        val splitTxs = createTransactionUseCase.execute(
            bob.id,
            CreateTransactionRequest(
                title = "Compra Dividida",
                amount = 10000,
                date = LocalDate.of(2026, 11, 13),
                accountId = bobPersonal.id,
                type = TransactionType.EXPENSE,
                status = TransactionStatus.COMPLETED,
                splits = listOf(
                    AccountSplitRequest(bobPersonal.id, 6000),
                    AccountSplitRequest(jointResp.id, 4000)
                )
            )
        )
        assertEquals(2, splitTxs.size)
        assertTrue(splitTxs.all { it.responsibleId == bob.id })

        // Bob pode editar e alternar status da despesa de Alice na conta conjunta sem alterar a categoria pessoal de Alice
        val editedByBob = updateTransactionUseCase.execute(
            bob.id,
            aliceExp.id,
            UpdateTransactionRequest(
                title = "Compra Atualizada por Bob",
                amount = 5001,
                date = LocalDate.of(2026, 11, 10),
                categoryId = aliceSecretCategory.id,
                status = TransactionStatus.COMPLETED
            )
        )
        assertEquals("Compra Atualizada por Bob", editedByBob.title)
        assertEquals(alice.id, editedByBob.responsibleId)
        assertEquals(aliceSecretCategory.id, editedByBob.categoryId)

        val toggledByBob = toggleTransactionStatusUseCase.execute(bob.id, aliceExp.id)
        assertEquals(TransactionStatus.PENDING, toggledByBob.status)
        toggleTransactionStatusUseCase.execute(bob.id, aliceExp.id) // volta a COMPLETED

        // Carol (não membro da conta conjunta) NÃO vê transações nem categorias pessoais usadas na conta conjunta restrita
        val carolGroupCategoryTxs = listCategoryTransactionsUseCase.execute(groupCategory.id, 11, 2026, carol.id)
        assertTrue(carolGroupCategoryTxs.isEmpty())
        val carolUsedCategories = getGroupUsedCategoriesUseCase.execute(group.id, carol.id)
        assertFalse(carolUsedCategories.any { it.id == aliceSecretCategory.id })

        // Alice vê tanto a transação da categoria do grupo quanto a usada por ela
        val aliceGroupCategoryTxs = listCategoryTransactionsUseCase.execute(groupCategory.id, 11, 2026, alice.id)
        assertEquals(1, aliceGroupCategoryTxs.size)

        // CalculateGroupDebtsUseCase não gera dívidas nem lança NPE com conta conjunta
        val debts = calculateGroupDebtsUseCase.execute(group.id, owner.id)
        assertEquals(0, debts.totalOwedByMe)
        assertEquals(0, debts.totalReceivableByMe)
        assertTrue(debts.monthlyBalances.all { it.userBalances.isEmpty() })

        // Resumo/rateio da conta conjunta em 11/2026
        val summary = getJointAccountSummaryUseCase.execute(group.id, jointResp.id, 11, 2026, alice.id)
        assertEquals(19001L, summary.totalExpenses)
        assertEquals(9001L, summary.completedExpenses)
        assertEquals(10000L, summary.pendingExpenses)
        assertEquals(20000L, summary.totalContributions)
        assertEquals(2, summary.memberCount)
        assertEquals(19001L, summary.memberShares.sumOf { it.shareAmount })

        // Cartão de crédito não pode ser vinculado a conta conjunta nem pago por conta conjunta sem acesso
        assertThrows<AccessDeniedException> {
            creditCardService.create(
                CreateCreditCardRequest(
                    name = "Card Conjunta",
                    accountId = jointResp.id,
                    limitAmount = 100000,
                    closingDay = 10,
                    dueDay = 20
                ),
                alice.id
            )
        }
        val aliceCard = creditCardRepository.save(
            CreditCardEntity(
                ownerId = alice.id,
                accountId = alicePersonal.id,
                name = "Card Alice",
                limitAmount = 100000,
                closingDay = 10,
                dueDay = 20
            )
        )
        assertThrows<AccessDeniedException> {
            creditCardService.update(
                aliceCard.id,
                UpdateCreditCardRequest(
                    name = "Card Alice",
                    accountId = jointResp.id,
                    limitAmount = 100000,
                    closingDay = 10,
                    dueDay = 20
                ),
                alice.id
            )
        }
        assertThrows<AccessDeniedException> {
            creditCardService.payInvoice(
                aliceCard.id,
                11,
                2026,
                PayInvoiceRequest(amount = 1000, accountId = jointResp.id, date = LocalDate.of(2026, 11, 15)),
                carol.id
            )
        }
        creditCardRepository.deleteById(aliceCard.id)

        // ListAdminUsersUseCase não falha com ownerId = null no banco
        val adminUsers = listAdminUsersUseCase.execute()
        assertTrue(adminUsers.any { it.id == owner.id })

        // Remover Bob do grupo remove Bob de joint_account_members
        removeMemberUseCase.execute(group.id, bob.email.normalized, owner.id)
        assertFalse(jointAccountMemberRepository.existsByAccountIdAndUserId(jointResp.id, bob.id))
        assertThrows<AccessDeniedException> {
            listAccountTransactionsUseCase.execute(jointResp.id, null, null, bob.id)
        }

        // UpdateJointAccount mantendo saldo transacional
        val beforeUpdateBalance = accountRepository.findById(jointResp.id)!!.balance
        val updatedJoint = updateJointAccountUseCase.execute(
            group.id,
            jointResp.id,
            UpdateJointAccountRequest(
                name = "Conjunta Atualizada",
                startBalance = 10000,
                balance = null,
                type = AccountType.CHECKING,
                tax = null,
                includeInTotal = true,
                memberIds = listOf(alice.id, carol.id)
            ),
            owner.id
        )
        assertEquals(beforeUpdateBalance, updatedJoint.balance)
        assertTrue(jointAccountMemberRepository.existsByAccountIdAndUserId(jointResp.id, carol.id))

        // Carol sai do grupo sem ter contas individuais vinculadas -> sai de joint_account_members
        leaveGroupUseCase.execute(group.id, carol.id)
        assertFalse(jointAccountMemberRepository.existsByAccountIdAndUserId(jointResp.id, carol.id))

        // DeleteJointAccount estorna aporte COMPLETED de Alice
        val aliceBalanceBeforeDelete = accountRepository.findById(alicePersonal.id)!!.balance
        deleteJointAccountUseCase.execute(group.id, jointResp.id, owner.id)
        assertNull(accountRepository.findById(jointResp.id))
        assertEquals(aliceBalanceBeforeDelete + 20000, accountRepository.findById(alicePersonal.id)!!.balance)

        // Cria nova conjunta e testa cascata no DeleteGroupUseCase
        val joint2 = createJointAccountUseCase.execute(
            group.id,
            CreateJointAccountRequest("Conjunta 2", 5000, AccountType.CHECKING, null, true, listOf(alice.id)),
            owner.id
        )
        deleteGroupUseCase.execute(group.id, owner.id)
        assertNull(accountRepository.findById(joint2.id))
        assertTrue(jointAccountMemberRepository.findAllByAccountId(joint2.id).isEmpty())

        // Testa DeleteUserUseCase e DeleteUserByAdminUseCase com grupo/conta conjunta
        val groupForAdminDelete = groupRepository.save(GroupEntity(name = "Grupo Admin Delete"))
        groupMemberRepository.save(GroupMemberEntity(groupId = groupForAdminDelete.id, userId = alice.id, role = GroupRole.OWNER))
        groupMemberRepository.save(GroupMemberEntity(groupId = groupForAdminDelete.id, userId = owner.id, role = GroupRole.MEMBER))
        val joint3 = createJointAccountUseCase.execute(
            groupForAdminDelete.id,
            CreateJointAccountRequest("Conjunta 3", 1000, AccountType.CHECKING, null, true, listOf(alice.id, owner.id)),
            alice.id
        )
        // Admin apaga owner (membro de joint3) -> limpa joint_account_members de owner
        val superAdmin = createUser("SuperAdmin")
        deleteUserByAdminUseCase.execute(superAdmin.id, owner.id)
        assertFalse(jointAccountMemberRepository.existsByAccountIdAndUserId(joint3.id, owner.id))

        // Alice (única dona restante de groupForAdminDelete) deleta própria conta -> cascata exclui joint3 e grupo
        deleteUserUseCase.execute(alice.id, alice.id)
        assertNull(accountRepository.findById(joint3.id))

        // Admin apaga usuário dono de grupo com conta conjunta -> cascata pelo EntityManager
        val owner2 = createUser("Owner2")
        val groupOwned = groupRepository.save(GroupEntity(name = "Grupo Owned"))
        groupMemberRepository.save(GroupMemberEntity(groupId = groupOwned.id, userId = owner2.id, role = GroupRole.OWNER))
        val joint4 = createJointAccountUseCase.execute(
            groupOwned.id,
            CreateJointAccountRequest("Conjunta 4", 1000, AccountType.CHECKING, null, true, listOf(owner2.id)),
            owner2.id
        )
        deleteUserByAdminUseCase.execute(superAdmin.id, owner2.id)
        assertNull(accountRepository.findById(joint4.id))
    }
}
