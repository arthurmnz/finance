package com.finance.application.use_case.transaction

import com.finance.application.dto.transaction.UpdateTransactionRequest
import com.finance.domain.entity.AccountEntity
import com.finance.domain.entity.CategoryEntity
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.entity.UserEntity
import com.finance.domain.enum.AccountType
import com.finance.domain.enum.CategoryLevel
import com.finance.domain.enum.CategoryType
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.InvalidTransactionException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.CreditCardInvoiceRepository
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
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdateTransactionUseCaseTest {

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(): T {
        Mockito.any<T>()
        return null as T
    }

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var userRepository: UserRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var groupMemberRepository: GroupMemberRepository
    private lateinit var creditCardInvoiceRepository: CreditCardInvoiceRepository
    private lateinit var creditCardRepository: CreditCardRepository
    private lateinit var useCase: UpdateTransactionUseCase

    @BeforeEach
    fun setup() {
        transactionRepository = mock(TransactionRepository::class.java)
        accountRepository = mock(AccountRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        categoryRepository = mock(CategoryRepository::class.java)
        groupMemberRepository = mock(GroupMemberRepository::class.java)
        creditCardInvoiceRepository = mock(CreditCardInvoiceRepository::class.java)
        creditCardRepository = mock(CreditCardRepository::class.java)

        useCase = UpdateTransactionUseCase(
            transactionRepository = transactionRepository,
            accountRepository = accountRepository,
            userRepository = userRepository,
            categoryRepository = categoryRepository,
            groupMemberRepository = groupMemberRepository,
            creditCardInvoiceRepository = creditCardInvoiceRepository,
            creditCardRepository = creditCardRepository
        )
    }

    @Test
    fun `should update generatesDebt on bank transfer between distinct users in shared group`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val userB = UserEntity(Name("Maria"), Name("Silva"), Email("maria@finance.com"), PasswordHash("hash"))
        val groupId = UUID.randomUUID()

        val accA = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Conta Arthur",
            startBalance = 50000,
            balance = 40000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = groupId
        )
        val accB = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userB.id,
            name = "Conta Maria",
            startBalance = 10000,
            balance = 20000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = groupId
        )

        val tx = TransactionEntity(
            title = "PIX",
            amount = 10000,
            date = LocalDate.of(2026, 11, 5),
            responsibleId = userA.id,
            accountId = accA.id,
            type = TransactionType.TRANSFER,
            destinationAccountId = accB.id,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            generatesDebt = false
        )

        `when`(transactionRepository.findById(tx.id)).thenReturn(tx)
        `when`(accountRepository.findById(accA.id)).thenReturn(accA)
        `when`(accountRepository.findById(accB.id)).thenReturn(accB)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(groupId, userA.id)).thenReturn(true)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(groupId, userB.id)).thenReturn(true)
        `when`(userRepository.findById(userA.id)).thenReturn(userA)
        `when`(transactionRepository.saveAll(anyNonNull())).thenAnswer { it.getArgument<List<TransactionEntity>>(0) }

        val updatedToTrue = useCase.execute(
            userA.id,
            tx.id,
            UpdateTransactionRequest(
                title = "PIX Empréstimo",
                amount = 10000,
                date = LocalDate.of(2026, 11, 5),
                status = TransactionStatus.COMPLETED,
                generatesDebt = true
            )
        )
        assertTrue(updatedToTrue.generatesDebt)

        val preservedWhenNull = useCase.execute(
            userA.id,
            tx.id,
            UpdateTransactionRequest(
                title = "PIX Empréstimo Editado",
                amount = 10000,
                date = LocalDate.of(2026, 11, 5),
                status = TransactionStatus.COMPLETED,
                generatesDebt = null
            )
        )
        assertTrue(preservedWhenNull.generatesDebt)

        val updatedToFalse = useCase.execute(
            userA.id,
            tx.id,
            UpdateTransactionRequest(
                title = "PIX Normal",
                amount = 10000,
                date = LocalDate.of(2026, 11, 5),
                status = TransactionStatus.COMPLETED,
                generatesDebt = false
            )
        )
        assertFalse(updatedToFalse.generatesDebt)
    }

    @Test
    fun `should reject generatesDebt true when updating non-transfer transaction`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val tx = TransactionEntity(
            title = "Almoço",
            amount = 5000,
            date = LocalDate.of(2026, 11, 5),
            responsibleId = userA.id,
            accountId = UUID.randomUUID(),
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED
        )

        `when`(transactionRepository.findById(tx.id)).thenReturn(tx)

        assertThrows<InvalidTransactionException> {
            useCase.execute(
                userA.id,
                tx.id,
                UpdateTransactionRequest(
                    title = "Almoço",
                    amount = 5000,
                    date = LocalDate.of(2026, 11, 5),
                    status = TransactionStatus.COMPLETED,
                    generatesDebt = true
                )
            )
        }
    }

    @Test
    fun `should reject update when amount is zero or negative`() {
        val userId = UUID.randomUUID()
        val txId = UUID.randomUUID()

        assertThrows<InvalidTransactionException> {
            useCase.execute(
                userId,
                txId,
                UpdateTransactionRequest(
                    title = "Zero",
                    amount = 0,
                    date = LocalDate.of(2026, 11, 5),
                    status = TransactionStatus.COMPLETED
                )
            )
        }

        assertThrows<InvalidTransactionException> {
            useCase.execute(
                userId,
                txId,
                UpdateTransactionRequest(
                    title = "Negativo",
                    amount = -100,
                    date = LocalDate.of(2026, 11, 5),
                    status = TransactionStatus.COMPLETED
                )
            )
        }
    }

    @Test
    fun `should revalidate generatesDebt when request generatesDebt is null and transaction generatesDebt is true`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val userB = UserEntity(Name("Maria"), Name("Silva"), Email("maria@finance.com"), PasswordHash("hash"))
        val groupId = UUID.randomUUID()

        val accA = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Conta Arthur",
            startBalance = 50000,
            balance = 40000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = groupId
        )
        // Conta destino foi desvinculada de grupo compartilhado após criação original
        val accBUnlinked = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userB.id,
            name = "Conta Maria",
            startBalance = 10000,
            balance = 20000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )

        val tx = TransactionEntity(
            title = "PIX Dívida",
            amount = 10000,
            date = LocalDate.of(2026, 11, 5),
            responsibleId = userA.id,
            accountId = accA.id,
            type = TransactionType.TRANSFER,
            destinationAccountId = accBUnlinked.id,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            generatesDebt = true
        )

        `when`(transactionRepository.findById(tx.id)).thenReturn(tx)
        `when`(accountRepository.findById(accA.id)).thenReturn(accA)
        `when`(accountRepository.findById(accBUnlinked.id)).thenReturn(accBUnlinked)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(groupId, userA.id)).thenReturn(true)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(groupId, userB.id)).thenReturn(false)

        assertThrows<InvalidTransactionException> {
            useCase.execute(
                userA.id,
                tx.id,
                UpdateTransactionRequest(
                    title = "PIX Dívida Editado",
                    amount = 10000,
                    date = LocalDate.of(2026, 11, 5),
                    status = TransactionStatus.COMPLETED,
                    generatesDebt = null
                )
            )
        }
    }

    @Test
    fun `should support GENERAL-only, GENERAL plus SPECIFIC, validation errors, and clearing categories on update`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val accA = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Conta Arthur",
            startBalance = 50000,
            balance = 45000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )
        val generalMoradia = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Moradia",
            description = null,
            userId = userA.id,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#7D39EB",
            icon = "🏠",
            level = CategoryLevel.GENERAL
        )
        val specificAluguel = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Aluguel",
            description = null,
            userId = userA.id,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#7D39EB",
            icon = "🔑",
            level = CategoryLevel.SPECIFIC,
            parentId = generalMoradia.id
        )
        val generalRenda = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Renda",
            description = null,
            userId = userA.id,
            groupId = null,
            type = CategoryType.INCOME,
            color = "#C6FF33",
            icon = "💰",
            level = CategoryLevel.GENERAL
        )
        val specificSalario = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Salário",
            description = null,
            userId = userA.id,
            groupId = null,
            type = CategoryType.INCOME,
            color = "#C6FF33",
            icon = "💼",
            level = CategoryLevel.SPECIFIC,
            parentId = generalRenda.id
        )

        val tx = TransactionEntity(
            title = "Pagamento",
            amount = 5000,
            date = LocalDate.of(2026, 11, 5),
            responsibleId = userA.id,
            accountId = accA.id,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED
        )

        `when`(transactionRepository.findById(tx.id)).thenReturn(tx)
        `when`(accountRepository.findById(accA.id)).thenReturn(accA)
        `when`(userRepository.findById(userA.id)).thenReturn(userA)
        `when`(categoryRepository.findById(generalMoradia.id)).thenReturn(generalMoradia)
        `when`(categoryRepository.findById(specificAluguel.id)).thenReturn(specificAluguel)
        `when`(categoryRepository.findById(generalRenda.id)).thenReturn(generalRenda)
        `when`(categoryRepository.findById(specificSalario.id)).thenReturn(specificSalario)
        `when`(transactionRepository.saveAll(anyNonNull())).thenAnswer { it.getArgument<List<TransactionEntity>>(0) }

        // Allow updating with ONLY GENERAL
        val generalOnlyUpdated = useCase.execute(
            userA.id,
            tx.id,
            UpdateTransactionRequest(
                title = "Gasto Geral Moradia",
                amount = 5000,
                date = LocalDate.of(2026, 11, 5),
                generalCategoryId = generalMoradia.id,
                status = TransactionStatus.COMPLETED
            )
        )
        assertEquals(generalMoradia.id, generalOnlyUpdated.categoryId)
        assertEquals(generalMoradia.id, generalOnlyUpdated.generalCategoryId)
        assertNull(generalOnlyUpdated.specificCategoryId)
        assertEquals("Moradia", generalOnlyUpdated.generalCategory?.name)
        assertNull(generalOnlyUpdated.specificCategory)
        assertEquals("Moradia", generalOnlyUpdated.category?.name)

        // Reject incompatible INCOME category on EXPENSE
        assertThrows<InvalidTransactionException> {
            useCase.execute(
                userA.id,
                tx.id,
                UpdateTransactionRequest(
                    title = "Pagamento",
                    amount = 5000,
                    date = LocalDate.of(2026, 11, 5),
                    categoryId = specificSalario.id,
                    status = TransactionStatus.COMPLETED
                )
            )
        }

        // Reject mismatched SPECIFIC that doesn't belong to GENERAL
        assertThrows<InvalidTransactionException> {
            useCase.execute(
                userA.id,
                tx.id,
                UpdateTransactionRequest(
                    title = "Inconsistente",
                    amount = 5000,
                    date = LocalDate.of(2026, 11, 5),
                    generalCategoryId = generalRenda.id,
                    specificCategoryId = specificAluguel.id,
                    status = TransactionStatus.COMPLETED
                )
            )
        }

        // Reject SPECIFIC passed in generalCategoryId
        assertThrows<InvalidTransactionException> {
            useCase.execute(
                userA.id,
                tx.id,
                UpdateTransactionRequest(
                    title = "Específica em Geral",
                    amount = 5000,
                    date = LocalDate.of(2026, 11, 5),
                    generalCategoryId = specificAluguel.id,
                    status = TransactionStatus.COMPLETED
                )
            )
        }

        // Reject GENERAL passed in specificCategoryId
        assertThrows<InvalidTransactionException> {
            useCase.execute(
                userA.id,
                tx.id,
                UpdateTransactionRequest(
                    title = "Geral em Específica",
                    amount = 5000,
                    date = LocalDate.of(2026, 11, 5),
                    specificCategoryId = generalMoradia.id,
                    status = TransactionStatus.COMPLETED
                )
            )
        }

        // Accept valid GENERAL + SPECIFIC and return enriched response
        val updated = useCase.execute(
            userA.id,
            tx.id,
            UpdateTransactionRequest(
                title = "Aluguel Novembro",
                amount = 5000,
                date = LocalDate.of(2026, 11, 5),
                generalCategoryId = generalMoradia.id,
                specificCategoryId = specificAluguel.id,
                status = TransactionStatus.COMPLETED
            )
        )
        assertEquals(specificAluguel.id, updated.categoryId)
        assertEquals(generalMoradia.id, updated.generalCategoryId)
        assertEquals(specificAluguel.id, updated.specificCategoryId)
        assertEquals("Moradia", updated.generalCategory?.name)
        assertEquals("Aluguel", updated.specificCategory?.name)
        assertEquals("Aluguel", updated.category?.name)
        assertEquals("Moradia", updated.category?.parent?.name)

        // Clear both categoryId and generalCategoryId when updated with null
        val cleared = useCase.execute(
            userA.id,
            tx.id,
            UpdateTransactionRequest(
                title = "Sem categoria",
                amount = 5000,
                date = LocalDate.of(2026, 11, 5),
                categoryId = null,
                generalCategoryId = null,
                specificCategoryId = null,
                status = TransactionStatus.COMPLETED
            )
        )
        assertNull(cleared.categoryId)
        assertNull(cleared.generalCategoryId)
        assertNull(cleared.specificCategoryId)
        assertNull(cleared.category)
        assertNull(cleared.generalCategory)
        assertNull(cleared.specificCategory)
    }

    @Test
    fun `should preserve both generalCategoryId and categoryId when regenerating future recurrences`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val accA = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Conta Arthur",
            startBalance = 50000,
            balance = 45000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )
        val generalMoradia = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Moradia",
            description = null,
            userId = userA.id,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#7D39EB",
            icon = "🏠",
            level = CategoryLevel.GENERAL
        )
        val specificAluguel = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Aluguel",
            description = null,
            userId = userA.id,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#7D39EB",
            icon = "🔑",
            level = CategoryLevel.SPECIFIC,
            parentId = generalMoradia.id
        )
        val recurrenceGroupId = UUID.randomUUID()
        val tx = TransactionEntity(
            title = "Aluguel",
            amount = 150000,
            date = LocalDate.of(2026, 11, 5),
            responsibleId = userA.id,
            accountId = accA.id,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.PENDING,
            recurrenceGroupId = recurrenceGroupId,
            recurrenceFrequency = com.finance.domain.enum.RecurrenceFrequency.MONTHLY,
            recurrenceInterval = 1
        )
        val savedBatches = mutableListOf<List<TransactionEntity>>()
        `when`(transactionRepository.findById(tx.id)).thenReturn(tx)
        `when`(transactionRepository.findFutureTransactionsByGroupId(recurrenceGroupId, tx.date)).thenReturn(emptyList())
        `when`(accountRepository.findById(accA.id)).thenReturn(accA)
        `when`(userRepository.findById(userA.id)).thenReturn(userA)
        `when`(categoryRepository.findById(generalMoradia.id)).thenReturn(generalMoradia)
        `when`(categoryRepository.findById(specificAluguel.id)).thenReturn(specificAluguel)
        `when`(transactionRepository.saveAll(anyNonNull())).thenAnswer {
            val list = it.getArgument<List<TransactionEntity>>(0)
            savedBatches.add(list)
            list
        }

        val updated = useCase.execute(
            userA.id,
            tx.id,
            UpdateTransactionRequest(
                title = "Aluguel Novo",
                amount = 160000,
                date = LocalDate.of(2026, 11, 5),
                generalCategoryId = generalMoradia.id,
                specificCategoryId = specificAluguel.id,
                status = TransactionStatus.PENDING,
                updateAllFuture = true,
                isRecurring = true,
                recurrenceFrequency = com.finance.domain.enum.RecurrenceFrequency.MONTHLY,
                recurrenceInterval = 1,
                recurrenceInstallments = 2
            )
        )

        assertEquals(specificAluguel.id, updated.categoryId)
        assertEquals(generalMoradia.id, updated.generalCategoryId)
        assertEquals(2, savedBatches.size)
        val regenerated = savedBatches[1]
        assertEquals(2, regenerated.size)
        assertTrue(regenerated.all { it.categoryId == specificAluguel.id && it.generalCategoryId == generalMoradia.id })
    }
}
