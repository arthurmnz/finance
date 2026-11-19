package com.finance.application.use_case.transaction

import com.finance.domain.entity.CategoryEntity
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.entity.UserEntity
import com.finance.domain.enum.CategoryLevel
import com.finance.domain.enum.CategoryType
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.application.service.AccountAccessService
import com.finance.domain.entity.AccountEntity
import com.finance.domain.enum.AccountType
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.JointAccountMemberRepository
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.repository.UserRepository
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ListResponsibleTransactionsUseCaseTest {

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var userRepository: UserRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var groupMemberRepository: GroupMemberRepository
    private lateinit var jointAccountMemberRepository: JointAccountMemberRepository
    private lateinit var useCase: ListResponsibleTransactionsUseCase

    private val user = UserEntity(
        id = UUID.randomUUID(),
        firstName = Name("Arthur"),
        lastName = Name("Menezes"),
        email = Email("arthur@finance.com"),
        passwordHash = PasswordHash("hash")
    )

    @BeforeEach
    fun setup() {
        transactionRepository = mock(TransactionRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        categoryRepository = mock(CategoryRepository::class.java)
        accountRepository = mock(AccountRepository::class.java)
        groupMemberRepository = mock(GroupMemberRepository::class.java)
        jointAccountMemberRepository = mock(JointAccountMemberRepository::class.java)

        useCase = ListResponsibleTransactionsUseCase(
            transactionRepository = transactionRepository,
            userRepository = userRepository,
            categoryRepository = categoryRepository,
            accountAccessService = AccountAccessService(groupMemberRepository, jointAccountMemberRepository),
            accountRepository = accountRepository
        )

        `when`(userRepository.findById(user.id)).thenReturn(user)
    }

    @Test
    fun `should filter transactions strictly by tx date competence instead of invoice month and include category summary`() {
        val catTransporte = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Transporte",
            description = null,
            userId = user.id,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#8E8EA0",
            icon = "🚗",
            level = CategoryLevel.GENERAL
        )
        val catUber = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Uber",
            description = "Transporte",
            userId = user.id,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#000000",
            icon = "car",
            level = CategoryLevel.SPECIFIC,
            parentId = catTransporte.id
        )
        `when`(categoryRepository.findByIdIn(listOf(catUber.id))).thenReturn(listOf(catUber))
        `when`(categoryRepository.findByIdIn(listOf(catTransporte.id))).thenReturn(listOf(catTransporte))
        `when`(categoryRepository.findByIdIn(listOf(catUber.id, catTransporte.id)))
            .thenReturn(listOf(catUber, catTransporte))
        `when`(categoryRepository.findByIdIn(listOf(catTransporte.id, catUber.id)))
            .thenReturn(listOf(catUber, catTransporte))

        // Compra no cartão realizada em 28 de outubro, cuja fatura é de novembro (GENERAL + SPECIFIC)
        val txOct = TransactionEntity(
            title = "Uber Outubro",
            amount = 3500,
            date = LocalDate.of(2026, 10, 28),
            responsibleId = user.id,
            accountId = UUID.randomUUID(),
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = catUber.id,
            status = TransactionStatus.COMPLETED,
            creditCardId = UUID.randomUUID(),
            invoiceMonth = 11,
            invoiceYear = 2026,
            generalCategoryId = catTransporte.id
        )

        // Compra no cartão realizada em 05 de novembro (apenas GENERAL)
        val txNov = TransactionEntity(
            title = "Transporte Geral Novembro",
            amount = 4500,
            date = LocalDate.of(2026, 11, 5),
            responsibleId = user.id,
            accountId = UUID.randomUUID(),
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = catTransporte.id,
            status = TransactionStatus.COMPLETED,
            creditCardId = UUID.randomUUID(),
            invoiceMonth = 11,
            invoiceYear = 2026,
            generalCategoryId = catTransporte.id
        )

        `when`(transactionRepository.findByResponsibleId(user.id)).thenReturn(listOf(txOct, txNov))

        // Quando filtrado por outubro de 2026 (month = 10, year = 2026)
        val octList = useCase.execute(user.id, month = 10, year = 2026)

        assertEquals(1, octList.size)
        assertEquals("Uber Outubro", octList[0].title)
        assertEquals(3500, octList[0].amount)
        assertNotNull(octList[0].category)
        assertEquals("Uber", octList[0].category?.name)
        assertEquals(catUber.id, octList[0].category?.id)
        assertEquals(CategoryLevel.SPECIFIC, octList[0].category?.level)
        assertEquals(catTransporte.id, octList[0].category?.parentId)
        assertEquals("Transporte", octList[0].category?.parent?.name)
        assertEquals(catTransporte.id, octList[0].generalCategoryId)
        assertEquals(catUber.id, octList[0].specificCategoryId)
        assertEquals("Transporte", octList[0].generalCategory?.name)
        assertEquals("Uber", octList[0].specificCategory?.name)

        // Quando filtrado por novembro de 2026 (month = 11, year = 2026) -> apenas GENERAL
        val novList = useCase.execute(user.id, month = 11, year = 2026)
        assertEquals(1, novList.size)
        assertEquals("Transporte Geral Novembro", novList[0].title)
        assertEquals(catTransporte.id, novList[0].categoryId)
        assertEquals(catTransporte.id, novList[0].generalCategoryId)
        assertNull(novList[0].specificCategoryId)
        assertEquals("Transporte", novList[0].generalCategory?.name)
        assertNull(novList[0].specificCategory)
    }

    @Test
    fun `should support flexible filtering by month only, year only, or all`() {
        val tx1 = TransactionEntity(
            title = "Tx 1",
            amount = 1000,
            date = LocalDate.of(2025, 10, 1),
            responsibleId = user.id,
            accountId = UUID.randomUUID(),
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED
        )
        val tx2 = TransactionEntity(
            title = "Tx 2",
            amount = 2000,
            date = LocalDate.of(2026, 10, 1),
            responsibleId = user.id,
            accountId = UUID.randomUUID(),
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED
        )

        `when`(transactionRepository.findByResponsibleId(user.id)).thenReturn(listOf(tx1, tx2))

        // Filtrar apenas por ano (2025)
        val yearOnly = useCase.execute(user.id, month = null, year = 2025)
        assertEquals(1, yearOnly.size)
        assertEquals("Tx 1", yearOnly[0].title)

        // Filtrar apenas por mês (10)
        val monthOnly = useCase.execute(user.id, month = 10, year = null)
        assertEquals(2, monthOnly.size)

        // Sem filtros
        val all = useCase.execute(user.id, month = null, year = null)
        assertEquals(2, all.size)
    }

    @Test
    fun `should hide transactions of joint account when access was revoked`() {
        val groupId = UUID.randomUUID()
        val joint = AccountEntity.createJoint(groupId, "Conjunta", 1000, AccountType.CHECKING)
        val personal = AccountEntity(ownerId = user.id, name = "Pessoal", startBalance = 0, type = AccountType.CHECKING, tax = null)
        fun tx(title: String, acc: UUID) = TransactionEntity(
            title = title, amount = 100, date = LocalDate.of(2026, 1, 1), responsibleId = user.id,
            accountId = acc, type = TransactionType.EXPENSE, destinationAccountId = null,
            categoryId = null, status = TransactionStatus.COMPLETED
        )
        val txJoint = tx("Conjunta", joint.id)
        val txPersonal = tx("Pessoal", personal.id)
        val txTransfer = TransactionEntity(
            title = "Transfer", amount = 100, date = LocalDate.of(2026, 1, 1), responsibleId = user.id,
            accountId = personal.id, type = TransactionType.TRANSFER, destinationAccountId = joint.id,
            categoryId = null, status = TransactionStatus.COMPLETED
        )
        `when`(transactionRepository.findByResponsibleId(user.id)).thenReturn(listOf(txJoint, txPersonal, txTransfer))
        `when`(accountRepository.findAllByIdIn(setOf(joint.id, personal.id)))
            .thenReturn(listOf(joint, personal))
        `when`(accountRepository.findAllByIdIn(listOf(joint.id, personal.id))).thenReturn(listOf(joint, personal))

        // access revoked: not a group member / joint member
        val result = useCase.execute(user.id, null, null)
        assertEquals(listOf("Pessoal"), result.map { it.title })

        // access granted
        `when`(groupMemberRepository.existsByGroupIdAndUserId(groupId, user.id)).thenReturn(true)
        `when`(jointAccountMemberRepository.existsByAccountIdAndUserId(joint.id, user.id)).thenReturn(true)
        assertEquals(3, useCase.execute(user.id, null, null).size)
    }
}
