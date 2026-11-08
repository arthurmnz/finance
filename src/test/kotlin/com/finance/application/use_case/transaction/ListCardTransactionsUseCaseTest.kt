package com.finance.application.use_case.transaction

import com.finance.domain.entity.CategoryEntity
import com.finance.domain.entity.CreditCardEntity
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.entity.UserEntity
import com.finance.domain.enum.CategoryType
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.CreditCardNotFoundException
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.CreditCardRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.repository.UserRepository
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ListCardTransactionsUseCaseTest {

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var creditCardRepository: CreditCardRepository
    private lateinit var groupMemberRepository: GroupMemberRepository
    private lateinit var userRepository: UserRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var useCase: ListCardTransactionsUseCase

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
        creditCardRepository = mock(CreditCardRepository::class.java)
        groupMemberRepository = mock(GroupMemberRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        categoryRepository = mock(CategoryRepository::class.java)

        useCase = ListCardTransactionsUseCase(
            transactionRepository = transactionRepository,
            creditCardRepository = creditCardRepository,
            groupMemberRepository = groupMemberRepository,
            userRepository = userRepository,
            categoryRepository = categoryRepository
        )

        `when`(userRepository.findAll()).thenReturn(listOf(user))
    }

    @Test
    fun `should list card transactions strictly by transaction date and enrich with category`() {
        val card = CreditCardEntity(
            id = UUID.randomUUID(),
            ownerId = user.id,
            accountId = UUID.randomUUID(),
            name = "Nubank",
            limitAmount = 500000,
            closingDay = 25,
            dueDay = 5
        )
        `when`(creditCardRepository.findById(card.id)).thenReturn(card)

        val catFood = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Restaurante",
            description = null,
            userId = user.id,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#FF0000",
            icon = "fork"
        )
        `when`(categoryRepository.findById(catFood.id)).thenReturn(catFood)

        // Compra feita em 28 de outubro (fatura de novembro)
        val txOct = TransactionEntity(
            title = "Jantar Outubro",
            amount = 12000,
            date = LocalDate.of(2026, 10, 28),
            responsibleId = user.id,
            accountId = card.accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = catFood.id,
            status = TransactionStatus.COMPLETED,
            creditCardId = card.id,
            invoiceMonth = 11,
            invoiceYear = 2026
        )

        // Compra feita em 02 de novembro
        val txNov = TransactionEntity(
            title = "Almoço Novembro",
            amount = 5000,
            date = LocalDate.of(2026, 11, 2),
            responsibleId = user.id,
            accountId = card.accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = catFood.id,
            status = TransactionStatus.COMPLETED,
            creditCardId = card.id,
            invoiceMonth = 11,
            invoiceYear = 2026
        )

        `when`(transactionRepository.findByCreditCardId(card.id)).thenReturn(listOf(txOct, txNov))

        val octList = useCase.execute(card.id, month = 10, year = 2026, authenticatedUserId = user.id)

        assertEquals(1, octList.size)
        assertEquals("Jantar Outubro", octList[0].title)
        assertEquals(12000, octList[0].amount)
        assertNotNull(octList[0].category)
        assertEquals("Restaurante", octList[0].category?.name)
    }

    @Test
    fun `should throw AccessDeniedException when user is not owner and not group member`() {
        val otherUserId = UUID.randomUUID()
        val card = CreditCardEntity(
            id = UUID.randomUUID(),
            ownerId = otherUserId,
            accountId = UUID.randomUUID(),
            name = "Cartão de Outro",
            limitAmount = 100000,
            closingDay = 10,
            dueDay = 20,
            groupId = null
        )
        `when`(creditCardRepository.findById(card.id)).thenReturn(card)

        assertThrows<AccessDeniedException> {
            useCase.execute(card.id, authenticatedUserId = user.id)
        }
    }

    @Test
    fun `should throw CreditCardNotFoundException when card does not exist`() {
        val nonExistentId = UUID.randomUUID()
        `when`(creditCardRepository.findById(nonExistentId)).thenReturn(null)

        assertThrows<CreditCardNotFoundException> {
            useCase.execute(nonExistentId, authenticatedUserId = user.id)
        }
    }
}
