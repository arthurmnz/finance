package com.finance.application.use_case.category

import com.finance.application.dto.category.CreateCategoryRequest
import com.finance.application.dto.category.UpdateCategoryRequest
import com.finance.application.service.GroupService
import com.finance.application.use_case.group.GetGroupUsedCategoriesUseCase
import com.finance.application.use_case.transaction.ListCategoryTransactionsUseCase
import com.finance.domain.entity.CategoryEntity
import com.finance.domain.entity.GroupEntity
import com.finance.domain.entity.GroupMemberEntity
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.entity.UserEntity
import com.finance.domain.enum.CategoryLevel
import com.finance.domain.enum.CategoryType
import com.finance.domain.enum.GroupRole
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.InvalidCategoryException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.CreditCardRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CategoryHierarchyUseCasesTest {

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(): T {
        Mockito.any<T>()
        return null as T
    }

    private lateinit var categoryRepository: CategoryRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var groupRepository: GroupRepository
    private lateinit var groupMemberRepository: GroupMemberRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var creditCardRepository: CreditCardRepository
    private lateinit var userRepository: UserRepository
    private lateinit var groupService: GroupService

    private lateinit var createCategoryUseCase: CreateCategoryUseCase
    private lateinit var updateCategoryUseCase: UpdateCategoryUseCase
    private lateinit var deleteCategoryUseCase: DeleteCategoryUseCase
    private lateinit var listUserCategoriesUseCase: ListUserCategoriesUseCase
    private lateinit var getGroupUsedCategoriesUseCase: GetGroupUsedCategoriesUseCase
    private lateinit var listCategoryTransactionsUseCase: ListCategoryTransactionsUseCase

    @BeforeEach
    fun setup() {
        categoryRepository = mock(CategoryRepository::class.java)
        transactionRepository = mock(TransactionRepository::class.java)
        groupRepository = mock(GroupRepository::class.java)
        groupMemberRepository = mock(GroupMemberRepository::class.java)
        accountRepository = mock(AccountRepository::class.java)
        creditCardRepository = mock(CreditCardRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        groupService = GroupService(groupRepository, groupMemberRepository)

        createCategoryUseCase = CreateCategoryUseCase(categoryRepository, groupService)
        updateCategoryUseCase = UpdateCategoryUseCase(categoryRepository, transactionRepository, groupService)
        deleteCategoryUseCase = DeleteCategoryUseCase(categoryRepository, transactionRepository, groupService)
        listUserCategoriesUseCase = ListUserCategoriesUseCase(categoryRepository)
        getGroupUsedCategoriesUseCase = GetGroupUsedCategoriesUseCase(
            groupRepository = groupRepository,
            groupMemberRepository = groupMemberRepository,
            accountRepository = accountRepository,
            creditCardRepository = creditCardRepository,
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository
        )
        listCategoryTransactionsUseCase = ListCategoryTransactionsUseCase(
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository,
            groupMemberRepository = groupMemberRepository,
            userRepository = userRepository,
            accountRepository = accountRepository,
            creditCardRepository = creditCardRepository
        )
    }

    @Test
    fun `create should infer level SPECIFIC when parentId is sent and enforce scope, 2-level depth, type, and group role`() {
        val userId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val groupId = UUID.randomUUID()

        val generalMoradia = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Moradia",
            description = null,
            userId = userId,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#7D39EB",
            icon = "🏠",
            level = CategoryLevel.GENERAL
        )
        val existingSpecific = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Aluguel",
            description = null,
            userId = userId,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#7D39EB",
            icon = "🔑",
            level = CategoryLevel.SPECIFIC,
            parentId = generalMoradia.id
        )
        val otherUserGeneral = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Outro",
            description = null,
            userId = otherUserId,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#000",
            icon = "📦",
            level = CategoryLevel.GENERAL
        )

        `when`(categoryRepository.findById(generalMoradia.id)).thenReturn(generalMoradia)
        `when`(categoryRepository.findById(existingSpecific.id)).thenReturn(existingSpecific)
        `when`(categoryRepository.findById(otherUserGeneral.id)).thenReturn(otherUserGeneral)
        `when`(categoryRepository.save(anyNonNull())).thenAnswer { it.getArgument<CategoryEntity>(0) }

        // Infers SPECIFIC when level is null and parentId is provided
        val created = createCategoryUseCase.execute(
            userId,
            CreateCategoryRequest(
                name = "Condomínio",
                description = null,
                type = CategoryType.EXPENSE,
                color = "#7D39EB",
                icon = "🏢",
                parentId = generalMoradia.id
            )
        )
        assertEquals(CategoryLevel.SPECIFIC, created.level)
        assertEquals(generalMoradia.id, created.parentId)
        assertEquals("Moradia", created.parent?.name)

        // Reject 3rd level (parent is SPECIFIC)
        assertThrows<InvalidCategoryException> {
            createCategoryUseCase.execute(
                userId,
                CreateCategoryRequest(
                    name = "Sub-sub",
                    description = null,
                    type = CategoryType.EXPENSE,
                    color = null,
                    icon = null,
                    parentId = existingSpecific.id
                )
            )
        }

        // Reject cross-user parent
        assertThrows<InvalidCategoryException> {
            createCategoryUseCase.execute(
                userId,
                CreateCategoryRequest(
                    name = "Invasão",
                    description = null,
                    type = CategoryType.EXPENSE,
                    color = null,
                    icon = null,
                    parentId = otherUserGeneral.id
                )
            )
        }

        // Reject incompatible type under EXPENSE general
        assertThrows<InvalidCategoryException> {
            createCategoryUseCase.execute(
                userId,
                CreateCategoryRequest(
                    name = "Salário",
                    description = null,
                    type = CategoryType.INCOME,
                    color = null,
                    icon = null,
                    parentId = generalMoradia.id
                )
            )
        }

        // Group IDOR check: MEMBER cannot create group category; ADMIN can
        `when`(groupRepository.findById(groupId)).thenReturn(GroupEntity(id = groupId, name = "Casa"))
        `when`(groupMemberRepository.findByGroupIdAndUserId(groupId, otherUserId)).thenReturn(
            GroupMemberEntity(groupId = groupId, userId = otherUserId, role = GroupRole.MEMBER)
        )
        assertThrows<AccessDeniedException> {
            createCategoryUseCase.execute(
                otherUserId,
                CreateCategoryRequest(
                    name = "Casa",
                    description = null,
                    type = CategoryType.EXPENSE,
                    color = null,
                    icon = null,
                    groupId = groupId
                )
            )
        }

        `when`(groupMemberRepository.findByGroupIdAndUserId(groupId, userId)).thenReturn(
            GroupMemberEntity(groupId = groupId, userId = userId, role = GroupRole.ADMIN)
        )
        val groupCat = createCategoryUseCase.execute(
            userId,
            CreateCategoryRequest(
                name = "Casa",
                description = null,
                type = CategoryType.EXPENSE,
                color = null,
                icon = null,
                groupId = groupId
            )
        )
        assertEquals(CategoryLevel.GENERAL, groupCat.level)
        assertEquals(groupId, groupCat.groupId)
    }

    @Test
    fun `update should prevent demoting GENERAL with children or changing GENERAL type when children are incompatible`() {
        val userId = UUID.randomUUID()
        val general = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Moradia",
            description = null,
            userId = userId,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#7D39EB",
            icon = "🏠",
            level = CategoryLevel.GENERAL
        )
        val anotherGeneral = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Outros",
            description = null,
            userId = userId,
            groupId = null,
            type = CategoryType.BOTH,
            color = "#8E8EA0",
            icon = "📦",
            level = CategoryLevel.GENERAL
        )
        val child = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Aluguel",
            description = null,
            userId = userId,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#7D39EB",
            icon = "🔑",
            level = CategoryLevel.SPECIFIC,
            parentId = general.id
        )

        `when`(categoryRepository.findById(general.id)).thenReturn(general)
        `when`(categoryRepository.findById(anotherGeneral.id)).thenReturn(anotherGeneral)
        `when`(categoryRepository.findById(child.id)).thenReturn(child)
        `when`(categoryRepository.existsByParentId(general.id)).thenReturn(true)
        `when`(categoryRepository.findByParentId(general.id)).thenReturn(listOf(child))
        `when`(categoryRepository.save(anyNonNull())).thenAnswer { it.getArgument<CategoryEntity>(0) }

        // Cannot demote GENERAL with children to SPECIFIC
        assertThrows<InvalidCategoryException> {
            updateCategoryUseCase.execute(
                userId,
                general.id,
                UpdateCategoryRequest(
                    name = "Moradia",
                    description = null,
                    type = CategoryType.EXPENSE,
                    color = null,
                    icon = null,
                    level = CategoryLevel.SPECIFIC,
                    parentId = anotherGeneral.id
                )
            )
        }

        // Cannot change GENERAL type from EXPENSE to INCOME when EXPENSE child exists
        assertThrows<InvalidCategoryException> {
            updateCategoryUseCase.execute(
                userId,
                general.id,
                UpdateCategoryRequest(
                    name = "Moradia",
                    description = null,
                    type = CategoryType.INCOME,
                    color = null,
                    icon = null
                )
            )
        }

        // Can widen GENERAL type to BOTH even with EXPENSE children
        val widened = updateCategoryUseCase.execute(
            userId,
            general.id,
            UpdateCategoryRequest(
                name = "Moradia",
                description = null,
                type = CategoryType.BOTH,
                color = "#7D39EB",
                icon = "🏠"
            )
        )
        assertEquals(CategoryType.BOTH, widened.type)

        // Updating SPECIFIC without sending level/parentId preserves SPECIFIC and its parent
        val renamedChild = updateCategoryUseCase.execute(
            userId,
            child.id,
            UpdateCategoryRequest(
                name = "Aluguel Apartamento",
                description = null,
                type = CategoryType.EXPENSE,
                color = "#7D39EB",
                icon = "🔑"
            )
        )
        assertEquals(CategoryLevel.SPECIFIC, renamedChild.level)
        assertEquals(general.id, renamedChild.parentId)
        assertEquals("Moradia", renamedChild.parent?.name)
    }

    @Test
    fun `delete should block GENERAL with children, check group role, and clear categoryId before deleting leaf`() {
        val userId = UUID.randomUUID()
        val memberId = UUID.randomUUID()
        val groupId = UUID.randomUUID()

        val general = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Moradia",
            description = null,
            userId = userId,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = null,
            icon = null,
            level = CategoryLevel.GENERAL
        )
        val specific = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Aluguel",
            description = null,
            userId = null,
            groupId = groupId,
            type = CategoryType.EXPENSE,
            color = null,
            icon = null,
            level = CategoryLevel.SPECIFIC,
            parentId = UUID.randomUUID()
        )

        `when`(categoryRepository.findById(general.id)).thenReturn(general)
        `when`(categoryRepository.existsByParentId(general.id)).thenReturn(true)

        assertThrows<InvalidCategoryException> {
            deleteCategoryUseCase.execute(userId, general.id)
        }
        verify(categoryRepository, never()).delete(general.id)

        `when`(categoryRepository.findById(specific.id)).thenReturn(specific)
        `when`(categoryRepository.existsByParentId(specific.id)).thenReturn(false)
        `when`(groupMemberRepository.findByGroupIdAndUserId(groupId, memberId)).thenReturn(
            GroupMemberEntity(groupId = groupId, userId = memberId, role = GroupRole.MEMBER)
        )

        assertThrows<AccessDeniedException> {
            deleteCategoryUseCase.execute(memberId, specific.id)
        }

        `when`(groupMemberRepository.findByGroupIdAndUserId(groupId, userId)).thenReturn(
            GroupMemberEntity(groupId = groupId, userId = userId, role = GroupRole.OWNER)
        )
        deleteCategoryUseCase.execute(userId, specific.id)

        verify(transactionRepository).clearCategoryId(specific.id)
        verify(categoryRepository).delete(specific.id)
    }

    @Test
    fun `list user categories should filter by level and parentId and enrich parent in batch`() {
        val userId = UUID.randomUUID()
        val general = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Moradia",
            description = null,
            userId = userId,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#7D39EB",
            icon = "🏠",
            level = CategoryLevel.GENERAL
        )
        val child = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Água",
            description = null,
            userId = userId,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#7D39EB",
            icon = "💧",
            level = CategoryLevel.SPECIFIC,
            parentId = general.id
        )

        `when`(categoryRepository.findByUserId(userId)).thenReturn(listOf(general, child))
        `when`(categoryRepository.findByUserIdAndLevel(userId, CategoryLevel.SPECIFIC)).thenReturn(listOf(child))
        `when`(categoryRepository.findByIdIn(listOf(general.id))).thenReturn(listOf(general))

        val specifics = listUserCategoriesUseCase.execute(userId, level = CategoryLevel.SPECIFIC, parentId = general.id)
        assertEquals(1, specifics.size)
        assertEquals("Água", specifics[0].name)
        assertEquals("Moradia", specifics[0].parent?.name)
    }

    @Test
    fun `listCategoryTransactions on GENERAL category should include all child SPECIFIC transactions with their parent`() {
        val userId = UUID.randomUUID()
        val user = UserEntity(
            id = userId,
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur@finance.com"),
            passwordHash = PasswordHash("hash")
        )
        val general = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Moradia",
            description = null,
            userId = userId,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#7D39EB",
            icon = "🏠",
            level = CategoryLevel.GENERAL
        )
        val childAluguel = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Aluguel",
            description = null,
            userId = userId,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#7D39EB",
            icon = "🔑",
            level = CategoryLevel.SPECIFIC,
            parentId = general.id
        )
        val childAgua = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Água",
            description = null,
            userId = userId,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#7D39EB",
            icon = "💧",
            level = CategoryLevel.SPECIFIC,
            parentId = general.id
        )

        val tx1 = TransactionEntity(
            title = "Aluguel Maio",
            amount = 150000,
            date = LocalDate.of(2026, 5, 5),
            responsibleId = userId,
            accountId = UUID.randomUUID(),
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = childAluguel.id,
            status = TransactionStatus.COMPLETED
        )
        val tx2 = TransactionEntity(
            title = "Conta de Água",
            amount = 8500,
            date = LocalDate.of(2026, 5, 10),
            responsibleId = userId,
            accountId = UUID.randomUUID(),
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = childAgua.id,
            status = TransactionStatus.COMPLETED
        )

        `when`(categoryRepository.findById(general.id)).thenReturn(general)
        `when`(categoryRepository.findByParentId(general.id)).thenReturn(listOf(childAluguel, childAgua))
        `when`(transactionRepository.findByCategoryIdIn(listOf(general.id, childAluguel.id, childAgua.id)))
            .thenReturn(listOf(tx1, tx2))
        `when`(userRepository.findAll()).thenReturn(listOf(user))

        val result = listCategoryTransactionsUseCase.execute(general.id, month = 5, year = 2026, authenticatedUserId = userId)
        assertEquals(2, result.size)

        val aluguelTx = result.first { it.title == "Aluguel Maio" }
        assertEquals("Aluguel", aluguelTx.category?.name)
        assertEquals("Moradia", aluguelTx.category?.parent?.name)
        assertEquals(CategoryLevel.SPECIFIC, aluguelTx.category?.level)

        val aguaTx = result.first { it.title == "Conta de Água" }
        assertEquals("Água", aguaTx.category?.name)
        assertEquals("Moradia", aguaTx.category?.parent?.name)
    }

    @Test
    fun `getGroupUsedCategories should include personal parent categories and populate hierarchy fields`() {
        val groupId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val accId = UUID.randomUUID()

        `when`(groupRepository.findById(groupId)).thenReturn(GroupEntity(id = groupId, name = "Casa"))
        `when`(groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)).thenReturn(true)
        `when`(accountRepository.findAllByGroupId(groupId)).thenReturn(
            listOf(
                com.finance.domain.entity.AccountEntity(
                    id = accId,
                    ownerId = userId,
                    name = "Conta Casa",
                    startBalance = 10000,
                    balance = 10000,
                    type = com.finance.domain.enum.AccountType.CHECKING,
                    tax = null,
                    groupId = groupId
                )
            )
        )
        `when`(creditCardRepository.findAllByGroupId(groupId)).thenReturn(emptyList())
        `when`(categoryRepository.findByGroupId(groupId)).thenReturn(emptyList())

        val personalParent = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Alimentação",
            description = null,
            userId = userId,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#E4FF8F",
            icon = "🍽️",
            level = CategoryLevel.GENERAL
        )
        val personalSpecific = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Supermercado",
            description = null,
            userId = userId,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#E4FF8F",
            icon = "🛒",
            level = CategoryLevel.SPECIFIC,
            parentId = personalParent.id
        )

        val tx = TransactionEntity(
            title = "Compras",
            amount = 20000,
            date = LocalDate.of(2026, 5, 12),
            responsibleId = userId,
            accountId = accId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = personalSpecific.id,
            status = TransactionStatus.COMPLETED
        )

        `when`(transactionRepository.findByAccountId(accId)).thenReturn(listOf(tx))
        `when`(categoryRepository.findByIdIn(listOf(personalSpecific.id))).thenReturn(listOf(personalSpecific))
        `when`(categoryRepository.findByIdIn(listOf(personalParent.id))).thenReturn(listOf(personalParent))

        val result = getGroupUsedCategoriesUseCase.execute(groupId, userId)
        assertEquals(2, result.size)

        val specificDto = result.first { it.id == personalSpecific.id }
        assertEquals(CategoryLevel.SPECIFIC, specificDto.level)
        assertEquals(personalParent.id, specificDto.parentId)
        assertNotNull(specificDto.parent)
        assertEquals("Alimentação", specificDto.parent.name)
        assertTrue(specificDto.isPersonal)

        val parentDto = result.first { it.id == personalParent.id }
        assertEquals(CategoryLevel.GENERAL, parentDto.level)
        assertNull(parentDto.parentId)
        assertNull(parentDto.parent)
    }

    @Test
    fun `update should reject SPECIFIC to GENERAL promotion with transactions, incompatible type change, invalid parent, cross-scope and 3rd level`() {
        val userId = UUID.randomUUID()
        val groupId = UUID.randomUUID()

        val general = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Moradia",
            description = null,
            userId = userId,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#7D39EB",
            icon = "🏠",
            level = CategoryLevel.GENERAL
        )
        val specific = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Aluguel",
            description = null,
            userId = userId,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#7D39EB",
            icon = "🔑",
            level = CategoryLevel.SPECIFIC,
            parentId = general.id
        )
        val anotherSpecific = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Água",
            description = null,
            userId = userId,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#7D39EB",
            icon = "💧",
            level = CategoryLevel.SPECIFIC,
            parentId = general.id
        )
        val groupGeneral = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Casa Grupo",
            description = null,
            userId = null,
            groupId = groupId,
            type = CategoryType.EXPENSE,
            color = "#7D39EB",
            icon = "🏠",
            level = CategoryLevel.GENERAL
        )

        val linkedExpenseTx = TransactionEntity(
            title = "Aluguel Maio",
            amount = 150000,
            date = LocalDate.of(2026, 5, 5),
            responsibleId = userId,
            accountId = UUID.randomUUID(),
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = specific.id,
            status = TransactionStatus.COMPLETED
        )

        `when`(categoryRepository.findById(general.id)).thenReturn(general)
        `when`(categoryRepository.findById(specific.id)).thenReturn(specific)
        `when`(categoryRepository.findById(anotherSpecific.id)).thenReturn(anotherSpecific)
        `when`(categoryRepository.findById(groupGeneral.id)).thenReturn(groupGeneral)
        `when`(categoryRepository.existsByParentId(general.id)).thenReturn(false)
        `when`(transactionRepository.findByCategoryId(specific.id)).thenReturn(listOf(linkedExpenseTx))

        // Block SPECIFIC -> GENERAL promotion when transactions exist
        assertThrows<InvalidCategoryException> {
            updateCategoryUseCase.execute(
                userId,
                specific.id,
                UpdateCategoryRequest(
                    name = "Aluguel",
                    description = null,
                    type = CategoryType.EXPENSE,
                    color = null,
                    icon = null,
                    level = CategoryLevel.GENERAL,
                    parentId = null
                )
            )
        }

        // Block changing SPECIFIC type to INCOME when EXPENSE transaction exists (under BOTH general)
        val bothGeneral = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Outros",
            description = null,
            userId = userId,
            groupId = null,
            type = CategoryType.BOTH,
            color = "#8E8EA0",
            icon = "📦",
            level = CategoryLevel.GENERAL
        )
        `when`(categoryRepository.findById(bothGeneral.id)).thenReturn(bothGeneral)
        assertThrows<InvalidCategoryException> {
            updateCategoryUseCase.execute(
                userId,
                specific.id,
                UpdateCategoryRequest(
                    name = "Aluguel",
                    description = null,
                    type = CategoryType.INCOME,
                    color = null,
                    icon = null,
                    level = CategoryLevel.SPECIFIC,
                    parentId = bothGeneral.id
                )
            )
        }

        // Reject GENERAL with parentId != null
        assertThrows<InvalidCategoryException> {
            updateCategoryUseCase.execute(
                userId,
                general.id,
                UpdateCategoryRequest(
                    name = "Moradia",
                    description = null,
                    type = CategoryType.EXPENSE,
                    color = null,
                    icon = null,
                    level = CategoryLevel.GENERAL,
                    parentId = bothGeneral.id
                )
            )
        }

        // Reject demoting GENERAL to SPECIFIC with parentId == null
        assertThrows<InvalidCategoryException> {
            updateCategoryUseCase.execute(
                userId,
                general.id,
                UpdateCategoryRequest(
                    name = "Moradia",
                    description = null,
                    type = CategoryType.EXPENSE,
                    color = null,
                    icon = null,
                    level = CategoryLevel.SPECIFIC,
                    parentId = null
                )
            )
        }

        // Reject cross-scope parent (personal category under group parent)
        assertThrows<InvalidCategoryException> {
            updateCategoryUseCase.execute(
                userId,
                specific.id,
                UpdateCategoryRequest(
                    name = "Aluguel",
                    description = null,
                    type = CategoryType.EXPENSE,
                    color = null,
                    icon = null,
                    level = CategoryLevel.SPECIFIC,
                    parentId = groupGeneral.id
                )
            )
        }

        // Reject 3rd level (parent is SPECIFIC)
        assertThrows<InvalidCategoryException> {
            updateCategoryUseCase.execute(
                userId,
                specific.id,
                UpdateCategoryRequest(
                    name = "Aluguel",
                    description = null,
                    type = CategoryType.EXPENSE,
                    color = null,
                    icon = null,
                    level = CategoryLevel.SPECIFIC,
                    parentId = anotherSpecific.id
                )
            )
        }
    }

    @Test
    fun `listCategoryTransactions should support SPECIFIC directly, deny unauthorized users, and isolate group transactions from private accounts`() {
        val userA = UUID.randomUUID()
        val userB = UUID.randomUUID()
        val intruder = UUID.randomUUID()
        val groupId = UUID.randomUUID()

        val userAEntity = UserEntity(
            id = userA,
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur@finance.com"),
            passwordHash = PasswordHash("hash")
        )
        val userBEntity = UserEntity(
            id = userB,
            firstName = Name("Maria"),
            lastName = Name("Silva"),
            email = Email("maria@finance.com"),
            passwordHash = PasswordHash("hash")
        )

        val personalGeneral = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Transporte",
            description = null,
            userId = userA,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#8E8EA0",
            icon = "🚗",
            level = CategoryLevel.GENERAL
        )
        val personalSpecific = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Uber",
            description = null,
            userId = userA,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#8E8EA0",
            icon = "🚕",
            level = CategoryLevel.SPECIFIC,
            parentId = personalGeneral.id
        )

        val txSpecific = TransactionEntity(
            title = "Corrida",
            amount = 2500,
            date = LocalDate.of(2026, 6, 1),
            responsibleId = userA,
            accountId = UUID.randomUUID(),
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = personalSpecific.id,
            status = TransactionStatus.COMPLETED
        )

        `when`(categoryRepository.findById(personalSpecific.id)).thenReturn(personalSpecific)
        `when`(transactionRepository.findByCategoryId(personalSpecific.id)).thenReturn(listOf(txSpecific))
        `when`(categoryRepository.findByIdIn(listOf(personalGeneral.id))).thenReturn(listOf(personalGeneral))
        `when`(userRepository.findAll()).thenReturn(listOf(userAEntity, userBEntity))

        // Intruder blocked with AccessDeniedException
        assertThrows<AccessDeniedException> {
            listCategoryTransactionsUseCase.execute(personalSpecific.id, authenticatedUserId = intruder)
        }

        // Owner gets SPECIFIC transaction with parent summary
        val specificResult = listCategoryTransactionsUseCase.execute(personalSpecific.id, authenticatedUserId = userA)
        assertEquals(1, specificResult.size)
        assertEquals("Uber", specificResult[0].category?.name)
        assertEquals("Transporte", specificResult[0].category?.parent?.name)

        // Group category isolation: private personal account tx of userB must NOT leak to userA
        val groupGeneral = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Casa",
            description = null,
            userId = null,
            groupId = groupId,
            type = CategoryType.EXPENSE,
            color = "#7D39EB",
            icon = "🏠",
            level = CategoryLevel.GENERAL
        )
        val groupSpecific = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Luz",
            description = null,
            userId = null,
            groupId = groupId,
            type = CategoryType.EXPENSE,
            color = "#7D39EB",
            icon = "⚡",
            level = CategoryLevel.SPECIFIC,
            parentId = groupGeneral.id
        )
        val groupAccount = com.finance.domain.entity.AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userB,
            name = "Conta Casa",
            startBalance = 10000,
            balance = 10000,
            type = com.finance.domain.enum.AccountType.CHECKING,
            tax = null,
            groupId = groupId
        )
        val privateAccountOfUserB = UUID.randomUUID()

        val txInGroupAccount = TransactionEntity(
            title = "Enel Grupo",
            amount = 15000,
            date = LocalDate.of(2026, 6, 2),
            responsibleId = userB,
            accountId = groupAccount.id,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = groupSpecific.id,
            status = TransactionStatus.COMPLETED
        )
        val txInUserBPrivateAccount = TransactionEntity(
            title = "Gasto Privado Maria",
            amount = 9999,
            date = LocalDate.of(2026, 6, 3),
            responsibleId = userB,
            accountId = privateAccountOfUserB,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = groupSpecific.id,
            status = TransactionStatus.COMPLETED
        )

        `when`(categoryRepository.findById(groupGeneral.id)).thenReturn(groupGeneral)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(groupId, userA)).thenReturn(true)
        `when`(categoryRepository.findByParentId(groupGeneral.id)).thenReturn(listOf(groupSpecific))
        `when`(transactionRepository.findByCategoryIdIn(listOf(groupGeneral.id, groupSpecific.id)))
            .thenReturn(listOf(txInGroupAccount, txInUserBPrivateAccount))
        `when`(accountRepository.findAllByGroupId(groupId)).thenReturn(listOf(groupAccount))
        `when`(creditCardRepository.findAllByGroupId(groupId)).thenReturn(emptyList())

        val groupResultForUserA = listCategoryTransactionsUseCase.execute(groupGeneral.id, authenticatedUserId = userA)
        assertEquals(1, groupResultForUserA.size)
        assertEquals("Enel Grupo", groupResultForUserA[0].title)
    }
}
