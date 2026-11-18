package com.finance.application.use_case.category

import com.finance.domain.entity.CategoryEntity
import com.finance.domain.entity.CreditCardEntity
import com.finance.domain.entity.GroupEntity
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.enum.CategoryLevel
import com.finance.domain.enum.CategoryType
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.GroupNotFoundException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.CreditCardRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
import com.finance.domain.repository.TransactionRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ListGroupCategoriesUseCaseTest {

    private lateinit var categoryRepository: CategoryRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var creditCardRepository: CreditCardRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var groupRepository: GroupRepository
    private lateinit var groupMemberRepository: GroupMemberRepository
    private lateinit var useCase: ListGroupCategoriesUseCase

    @BeforeEach
    fun setup() {
        categoryRepository = mock(CategoryRepository::class.java)
        accountRepository = mock(AccountRepository::class.java)
        creditCardRepository = mock(CreditCardRepository::class.java)
        transactionRepository = mock(TransactionRepository::class.java)
        groupRepository = mock(GroupRepository::class.java)
        groupMemberRepository = mock(GroupMemberRepository::class.java)

        useCase = ListGroupCategoriesUseCase(
            categoryRepository = categoryRepository,
            accountRepository = accountRepository,
            creditCardRepository = creditCardRepository,
            transactionRepository = transactionRepository,
            groupRepository = groupRepository,
            groupMemberRepository = groupMemberRepository
        )
    }

    @Test
    fun `should return explicit group categories plus personal specific categories and their general parents`() {
        val groupId = UUID.randomUUID()
        val userAId = UUID.randomUUID()
        val userBId = UUID.randomUUID()

        val group = GroupEntity(id = groupId, name = "Família")
        `when`(groupRepository.findById(groupId)).thenReturn(group)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(groupId, userBId)).thenReturn(true)

        val catMoradiaGrupo = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Moradia",
            description = null,
            userId = null,
            groupId = groupId,
            type = CategoryType.EXPENSE,
            color = "#7D39EB",
            icon = "🏠",
            level = CategoryLevel.GENERAL
        )
        val catCondo = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Condomínio",
            description = null,
            userId = null,
            groupId = groupId,
            type = CategoryType.EXPENSE,
            color = "#00FF00",
            icon = "🏢",
            level = CategoryLevel.SPECIFIC,
            parentId = catMoradiaGrupo.id
        )
        `when`(categoryRepository.findByGroupId(groupId)).thenReturn(listOf(catMoradiaGrupo, catCondo))

        val card = CreditCardEntity(
            id = UUID.randomUUID(),
            ownerId = userAId,
            accountId = UUID.randomUUID(),
            name = "Cartão Familiar",
            limitAmount = 1000000,
            closingDay = 25,
            dueDay = 5,
            groupId = groupId
        )
        `when`(creditCardRepository.findAllByGroupId(groupId)).thenReturn(listOf(card))
        `when`(accountRepository.findAllByGroupId(groupId)).thenReturn(emptyList())

        val catTransporte = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Transporte",
            description = null,
            userId = userBId,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#8E8EA0",
            icon = "🚗",
            level = CategoryLevel.GENERAL
        )
        val catAlimentacao = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Alimentação",
            description = null,
            userId = userBId,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#E4FF8F",
            icon = "🍽️",
            level = CategoryLevel.GENERAL
        )
        val catUber = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Uber",
            description = null,
            userId = userBId,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#000000",
            icon = "🚕",
            level = CategoryLevel.SPECIFIC,
            parentId = catTransporte.id
        )
        val catIfood = CategoryEntity(
            id = UUID.randomUUID(),
            name = "iFood",
            description = null,
            userId = userBId,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#EA1D2C",
            icon = "🛵",
            level = CategoryLevel.SPECIFIC,
            parentId = catAlimentacao.id
        )

        `when`(categoryRepository.findByIdIn(listOf(catUber.id, catIfood.id))).thenReturn(listOf(catUber, catIfood))
        `when`(categoryRepository.findByIdIn(listOf(catIfood.id, catUber.id))).thenReturn(listOf(catUber, catIfood))
        `when`(categoryRepository.findByIdIn(listOf(catTransporte.id, catAlimentacao.id))).thenReturn(listOf(catTransporte, catAlimentacao))
        `when`(categoryRepository.findByIdIn(listOf(catAlimentacao.id, catTransporte.id))).thenReturn(listOf(catTransporte, catAlimentacao))

        val tx1 = TransactionEntity(
            title = "Corrida",
            amount = 3000,
            date = LocalDate.of(2026, 10, 20),
            responsibleId = userBId,
            accountId = card.accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = catUber.id,
            status = TransactionStatus.COMPLETED,
            creditCardId = card.id
        )
        val tx2 = TransactionEntity(
            title = "Jantar",
            amount = 5000,
            date = LocalDate.of(2026, 10, 21),
            responsibleId = userBId,
            accountId = card.accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = catIfood.id,
            status = TransactionStatus.COMPLETED,
            creditCardId = card.id
        )
        `when`(transactionRepository.findByCreditCardId(card.id)).thenReturn(listOf(tx1, tx2))

        val result = useCase.execute(groupId, userBId)

        // Deve incluir: Moradia (grupo), Condomínio (grupo), Uber (pessoal), iFood (pessoal),
        // Transporte (pai pessoal incluído) e Alimentação (pai pessoal incluído)
        assertEquals(6, result.size)
        val names = result.map { it.name }
        assertTrue(names.containsAll(listOf("Moradia", "Condomínio", "Uber", "iFood", "Transporte", "Alimentação")))

        val uberDto = result.first { it.id == catUber.id }
        assertEquals(CategoryLevel.SPECIFIC, uberDto.level)
        assertEquals(catTransporte.id, uberDto.parentId)
        assertNotNull(uberDto.parent)
        assertEquals("Transporte", uberDto.parent.name)

        // Filtro por level = GENERAL deve incluir os pais pessoais também
        val generalsOnly = useCase.execute(groupId, userBId, level = CategoryLevel.GENERAL)
        assertEquals(3, generalsOnly.size)
        assertTrue(generalsOnly.map { it.name }.containsAll(listOf("Moradia", "Transporte", "Alimentação")))

        // Filtro por parentId = catTransporte.id
        val transporteChildren = useCase.execute(groupId, userBId, parentId = catTransporte.id)
        assertEquals(1, transporteChildren.size)
        assertEquals("Uber", transporteChildren[0].name)
    }

    @Test
    fun `should throw GroupNotFoundException when group does not exist`() {
        val groupId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        `when`(groupRepository.findById(groupId)).thenReturn(null)

        assertThrows<GroupNotFoundException> {
            useCase.execute(groupId, userId)
        }
    }

    @Test
    fun `should throw AccessDeniedException when user is not member of group`() {
        val groupId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        val group = GroupEntity(id = groupId, name = "Família")
        `when`(groupRepository.findById(groupId)).thenReturn(group)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)).thenReturn(false)

        val exception = assertThrows<AccessDeniedException> {
            useCase.execute(groupId, userId)
        }
        assertEquals("Você não tem acesso a este grupo.", exception.message)
    }
}
