package com.finance.application.use_case.transaction

import com.finance.domain.entity.CategoryEntity
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.entity.UserEntity
import com.finance.domain.enum.CategoryType
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
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

class ListResponsibleTransactionsUseCaseTest {

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var userRepository: UserRepository
    private lateinit var categoryRepository: CategoryRepository
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

        useCase = ListResponsibleTransactionsUseCase(
            transactionRepository = transactionRepository,
            userRepository = userRepository,
            categoryRepository = categoryRepository
        )

        `when`(userRepository.findById(user.id)).thenReturn(user)
    }

    @Test
    fun `should filter transactions strictly by tx date competence instead of invoice month and include category summary`() {
        val catUber = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Uber",
            description = "Transporte",
            userId = user.id,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#000000",
            icon = "car"
        )
        `when`(categoryRepository.findById(catUber.id)).thenReturn(catUber)

        // Compra no cartão realizada em 28 de outubro, cuja fatura é de novembro
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
            invoiceYear = 2026
        )

        // Compra no cartão realizada em 05 de novembro
        val txNov = TransactionEntity(
            title = "Uber Novembro",
            amount = 4500,
            date = LocalDate.of(2026, 11, 5),
            responsibleId = user.id,
            accountId = UUID.randomUUID(),
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = catUber.id,
            status = TransactionStatus.COMPLETED,
            creditCardId = UUID.randomUUID(),
            invoiceMonth = 11,
            invoiceYear = 2026
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

        // Quando filtrado por novembro de 2026 (month = 11, year = 2026)
        val novList = useCase.execute(user.id, month = 11, year = 2026)
        assertEquals(1, novList.size)
        assertEquals("Uber Novembro", novList[0].title)
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
}
