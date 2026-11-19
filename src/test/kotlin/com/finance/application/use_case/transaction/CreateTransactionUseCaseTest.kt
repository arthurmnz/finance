package com.finance.application.use_case.transaction

import com.finance.application.dto.transaction.AccountSplitRequest
import com.finance.application.dto.transaction.CreateTransactionRequest
import com.finance.application.service.AccountAccessService
import com.finance.application.service.CreditCardService
import com.finance.domain.entity.AccountEntity
import com.finance.domain.entity.CategoryEntity
import com.finance.domain.entity.CreditCardEntity
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.entity.UserEntity
import com.finance.domain.enum.AccountType
import com.finance.domain.enum.CategoryLevel
import com.finance.domain.enum.CategoryType
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.exception.InvalidTransactionException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.GroupMemberRepository
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CreateTransactionUseCaseTest {

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(): T {
        Mockito.any<T>()
        return null as T
    }

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var creditCardService: CreditCardService
    private lateinit var userRepository: UserRepository
    private lateinit var groupMemberRepository: GroupMemberRepository
    private lateinit var jointAccountMemberRepository: JointAccountMemberRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var accountAccessService: AccountAccessService
    private lateinit var useCase: CreateTransactionUseCase

    @BeforeEach
    fun setup() {
        transactionRepository = mock(TransactionRepository::class.java)
        accountRepository = mock(AccountRepository::class.java)
        creditCardService = mock(CreditCardService::class.java)
        userRepository = mock(UserRepository::class.java)
        groupMemberRepository = mock(GroupMemberRepository::class.java)
        jointAccountMemberRepository = mock(JointAccountMemberRepository::class.java)
        categoryRepository = mock(CategoryRepository::class.java)
        accountAccessService = AccountAccessService(groupMemberRepository, jointAccountMemberRepository)

        useCase = CreateTransactionUseCase(
            transactionRepository = transactionRepository,
            accountRepository = accountRepository,
            creditCardService = creditCardService,
            userRepository = userRepository,
            groupMemberRepository = groupMemberRepository,
            categoryRepository = categoryRepository,
            accountAccessService = accountAccessService
        )
    }

    @Test
    fun `should create TRANSFER with generatesDebt true between two distinct users in shared group`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val userB = UserEntity(Name("Maria"), Name("Silva"), Email("maria@finance.com"), PasswordHash("hash"))
        val groupId = UUID.randomUUID()

        val sourceAcc = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Conta Arthur",
            startBalance = 50000,
            balance = 50000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = groupId
        )
        val destAcc = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userB.id,
            name = "Conta Maria",
            startBalance = 10000,
            balance = 10000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = groupId
        )

        `when`(accountRepository.findById(sourceAcc.id)).thenReturn(sourceAcc)
        `when`(accountRepository.findById(destAcc.id)).thenReturn(destAcc)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(groupId, userA.id)).thenReturn(true)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(groupId, userB.id)).thenReturn(true)
        `when`(userRepository.findAll()).thenReturn(listOf(userA, userB))
        `when`(transactionRepository.saveAll(anyNonNull())).thenAnswer { it.getArgument<List<TransactionEntity>>(0) }

        val request = CreateTransactionRequest(
            title = "Empréstimo Maria",
            amount = 15000,
            date = LocalDate.of(2026, 11, 10),
            accountId = sourceAcc.id,
            type = TransactionType.TRANSFER,
            destinationAccountId = destAcc.id,
            status = TransactionStatus.COMPLETED,
            generatesDebt = true
        )

        val result = useCase.execute(userA.id, request)

        assertEquals(1, result.size)
        assertTrue(result[0].generatesDebt)
        assertEquals(TransactionType.TRANSFER, result[0].type)
        assertEquals(35000, sourceAcc.balance)
        assertEquals(25000, destAcc.balance)
        verify(accountRepository).save(sourceAcc)
        verify(accountRepository).save(destAcc)
    }

    @Test
    fun `should reject generatesDebt true on non-transfer or credit card transactions`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val cardId = UUID.randomUUID()
        val accountId = UUID.randomUUID()

        assertThrows<InvalidTransactionException> {
            useCase.execute(
                userA.id,
                CreateTransactionRequest(
                    title = "Mercado",
                    amount = 5000,
                    date = LocalDate.of(2026, 11, 10),
                    accountId = accountId,
                    type = TransactionType.EXPENSE,
                    generatesDebt = true
                )
            )
        }

        val card = CreditCardEntity(
            id = cardId,
            ownerId = userA.id,
            accountId = accountId,
            name = "Cartão",
            limitAmount = 100000,
            closingDay = 25,
            dueDay = 5
        )
        `when`(creditCardService.requireCardAccess(cardId, userA.id)).thenReturn(card)

        assertThrows<InvalidTransactionException> {
            useCase.execute(
                userA.id,
                CreateTransactionRequest(
                    title = "Transferência no cartão?",
                    amount = 5000,
                    date = LocalDate.of(2026, 11, 10),
                    accountId = accountId,
                    type = TransactionType.TRANSFER,
                    destinationAccountId = UUID.randomUUID(),
                    creditCardId = cardId,
                    generatesDebt = true
                )
            )
        }
    }

    @Test
    fun `should validate destination account existence, difference and access on transfers`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val userB = UserEntity(Name("Maria"), Name("Silva"), Email("maria@finance.com"), PasswordHash("hash"))

        val sourceAcc = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Conta Arthur",
            startBalance = 50000,
            balance = 50000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )
        val foreignAcc = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userB.id,
            name = "Conta Maria Privada",
            startBalance = 10000,
            balance = 10000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )

        `when`(accountRepository.findById(sourceAcc.id)).thenReturn(sourceAcc)
        `when`(accountRepository.findById(foreignAcc.id)).thenReturn(foreignAcc)

        // Sem destino
        assertThrows<InvalidTransactionException> {
            useCase.execute(
                userA.id,
                CreateTransactionRequest(
                    title = "Sem destino",
                    amount = 5000,
                    date = LocalDate.of(2026, 11, 10),
                    accountId = sourceAcc.id,
                    type = TransactionType.TRANSFER,
                    destinationAccountId = null
                )
            )
        }

        // Mesma conta origem/destino
        assertThrows<InvalidTransactionException> {
            useCase.execute(
                userA.id,
                CreateTransactionRequest(
                    title = "Mesma conta",
                    amount = 5000,
                    date = LocalDate.of(2026, 11, 10),
                    accountId = sourceAcc.id,
                    type = TransactionType.TRANSFER,
                    destinationAccountId = sourceAcc.id
                )
            )
        }

        // Destino inexistente
        val missingId = UUID.randomUUID()
        `when`(accountRepository.findById(missingId)).thenReturn(null)
        assertThrows<AccountNotFoundException> {
            useCase.execute(
                userA.id,
                CreateTransactionRequest(
                    title = "Destino inexistente",
                    amount = 5000,
                    date = LocalDate.of(2026, 11, 10),
                    accountId = sourceAcc.id,
                    type = TransactionType.TRANSFER,
                    destinationAccountId = missingId
                )
            )
        }

        // Sem permissão na conta de destino
        assertThrows<AccessDeniedException> {
            useCase.execute(
                userA.id,
                CreateTransactionRequest(
                    title = "Destino sem acesso",
                    amount = 5000,
                    date = LocalDate.of(2026, 11, 10),
                    accountId = sourceAcc.id,
                    type = TransactionType.TRANSFER,
                    destinationAccountId = foreignAcc.id
                )
            )
        }
    }

    @Test
    fun `should reject generatesDebt true when non-owner initiates, same owner, or no shared group`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val userB = UserEntity(Name("Maria"), Name("Silva"), Email("maria@finance.com"), PasswordHash("hash"))
        val groupId = UUID.randomUUID()

        val accA = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Conta Arthur Grupo",
            startBalance = 50000,
            balance = 50000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = groupId
        )
        val accA2Personal = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Conta Arthur Pessoal 2",
            startBalance = 10000,
            balance = 10000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )
        val accB = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userB.id,
            name = "Conta Maria Grupo",
            startBalance = 10000,
            balance = 10000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = groupId
        )
        val accAPersonal = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Conta Arthur Pessoal",
            startBalance = 50000,
            balance = 50000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )
        val accBPersonal = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userB.id,
            name = "Conta Maria Pessoal",
            startBalance = 10000,
            balance = 10000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )

        `when`(accountRepository.findById(accA.id)).thenReturn(accA)
        `when`(accountRepository.findById(accA2Personal.id)).thenReturn(accA2Personal)
        `when`(accountRepository.findById(accB.id)).thenReturn(accB)
        `when`(accountRepository.findById(accAPersonal.id)).thenReturn(accAPersonal)
        `when`(accountRepository.findById(accBPersonal.id)).thenReturn(accBPersonal)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(groupId, userA.id)).thenReturn(true)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(groupId, userB.id)).thenReturn(true)

        // 1. Usuário B (não titular de accA, embora membro do grupo) tentando criar dívida saindo da conta de Arthur
        assertThrows<AccessDeniedException> {
            useCase.execute(
                userB.id,
                CreateTransactionRequest(
                    title = "Tentativa indevida",
                    amount = 5000,
                    date = LocalDate.of(2026, 11, 10),
                    accountId = accA.id,
                    type = TransactionType.TRANSFER,
                    destinationAccountId = accB.id,
                    generatesDebt = true
                )
            )
        }

        // 2. Mesma pessoa (Arthur -> outra conta de Arthur) com generatesDebt = true
        assertThrows<InvalidTransactionException> {
            useCase.execute(
                userA.id,
                CreateTransactionRequest(
                    title = "Entre minhas contas",
                    amount = 5000,
                    date = LocalDate.of(2026, 11, 10),
                    accountId = accA.id,
                    type = TransactionType.TRANSFER,
                    destinationAccountId = accA2Personal.id,
                    generatesDebt = true
                )
            )
        }

        // 3. Sem vínculo de grupo compartilhado (destAcc sem grupo, mas supondo acesso — neste caso nem acesso teria sem grupo, mas se source também sem grupo)
        // Caso onde destAcc está em um grupo em que o sourceOwner NÃO participa
        val otherGroupId = UUID.randomUUID()
        val accBOtherGroup = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userB.id,
            name = "Conta Maria Outro Grupo",
            startBalance = 10000,
            balance = 10000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = otherGroupId
        )
        `when`(accountRepository.findById(accBOtherGroup.id)).thenReturn(accBOtherGroup)
        // Arthur não é membro de otherGroupId -> falha por AccessDeniedException no destino;
        // E se Arthur fosse membro de otherGroupId mas Maria não fosse (impossível normalmente, mas valida bothInSharedGroup):
        `when`(groupMemberRepository.existsByGroupIdAndUserId(otherGroupId, userA.id)).thenReturn(true)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(otherGroupId, userB.id)).thenReturn(false)

        assertThrows<InvalidTransactionException> {
            useCase.execute(
                userA.id,
                CreateTransactionRequest(
                    title = "Sem grupo comum válido",
                    amount = 5000,
                    date = LocalDate.of(2026, 11, 10),
                    accountId = accAPersonal.id,
                    type = TransactionType.TRANSFER,
                    destinationAccountId = accBOtherGroup.id,
                    generatesDebt = true
                )
            )
        }
        verify(transactionRepository, never()).saveAll(anyNonNull())
    }

    @Test
    fun `should allow normal transfer without generatesDebt between own accounts`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val acc1 = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Corrente",
            startBalance = 50000,
            balance = 50000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )
        val acc2 = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Carteira",
            startBalance = 10000,
            balance = 10000,
            type = AccountType.WALLET,
            tax = null,
            groupId = null
        )

        `when`(accountRepository.findById(acc1.id)).thenReturn(acc1)
        `when`(accountRepository.findById(acc2.id)).thenReturn(acc2)
        `when`(userRepository.findAll()).thenReturn(listOf(userA))
        `when`(transactionRepository.saveAll(anyNonNull())).thenAnswer { it.getArgument<List<TransactionEntity>>(0) }

        val result = useCase.execute(
            userA.id,
            CreateTransactionRequest(
                title = "Saque",
                amount = 10000,
                date = LocalDate.of(2026, 11, 10),
                accountId = acc1.id,
                type = TransactionType.TRANSFER,
                destinationAccountId = acc2.id,
                status = TransactionStatus.COMPLETED,
                generatesDebt = false
            )
        )

        assertEquals(1, result.size)
        assertFalse(result[0].generatesDebt)
        assertEquals(40000, acc1.balance)
        assertEquals(20000, acc2.balance)
    }

    @Test
    fun `should reject transaction when amount is zero or negative`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val accountId = UUID.randomUUID()

        assertThrows<InvalidTransactionException> {
            useCase.execute(
                userA.id,
                CreateTransactionRequest(
                    title = "Zero",
                    amount = 0,
                    date = LocalDate.of(2026, 11, 10),
                    accountId = accountId,
                    type = TransactionType.EXPENSE
                )
            )
        }

        assertThrows<InvalidTransactionException> {
            useCase.execute(
                userA.id,
                CreateTransactionRequest(
                    title = "Negativo",
                    amount = -500,
                    date = LocalDate.of(2026, 11, 10),
                    accountId = accountId,
                    type = TransactionType.EXPENSE
                )
            )
        }
        verify(transactionRepository, never()).saveAll(anyNonNull())
    }

    @Test
    fun `should not leak account name in AccessDeniedException messages`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val userB = UserEntity(Name("Maria"), Name("Silva"), Email("maria@finance.com"), PasswordHash("hash"))

        val accA = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Conta Arthur",
            startBalance = 50000,
            balance = 50000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )
        val secretDestAcc = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userB.id,
            name = "Conta Secreta Maria",
            startBalance = 10000,
            balance = 10000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )

        `when`(accountRepository.findById(accA.id)).thenReturn(accA)
        `when`(accountRepository.findById(secretDestAcc.id)).thenReturn(secretDestAcc)

        val exSource = assertThrows<AccessDeniedException> {
            useCase.execute(
                userA.id,
                CreateTransactionRequest(
                    title = "Tentativa origem",
                    amount = 1000,
                    date = LocalDate.of(2026, 11, 10),
                    accountId = secretDestAcc.id,
                    type = TransactionType.EXPENSE
                )
            )
        }
        assertFalse(exSource.message!!.contains("Conta Secreta Maria"))

        val exDest = assertThrows<AccessDeniedException> {
            useCase.execute(
                userA.id,
                CreateTransactionRequest(
                    title = "Tentativa destino",
                    amount = 1000,
                    date = LocalDate.of(2026, 11, 10),
                    accountId = accA.id,
                    type = TransactionType.TRANSFER,
                    destinationAccountId = secretDestAcc.id
                )
            )
        }
        assertFalse(exDest.message!!.contains("Conta Secreta Maria"))
    }

    @Test
    fun `should allow linking transaction only to GENERAL category via generalCategoryId or legacy categoryId`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val acc = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Conta Principal",
            startBalance = 500000,
            balance = 500000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )
        val generalCategory = CategoryEntity(
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
        `when`(categoryRepository.findById(generalCategory.id)).thenReturn(generalCategory)
        `when`(accountRepository.findById(acc.id)).thenReturn(acc)
        `when`(userRepository.findAll()).thenReturn(listOf(userA))
        `when`(transactionRepository.saveAll(anyNonNull())).thenAnswer { it.getArgument<List<TransactionEntity>>(0) }

        val viaGeneralField = useCase.execute(
            userA.id,
            CreateTransactionRequest(
                title = "Taxa Residencial",
                amount = 15000,
                date = LocalDate.of(2026, 11, 10),
                accountId = acc.id,
                type = TransactionType.EXPENSE,
                generalCategoryId = generalCategory.id,
                status = TransactionStatus.COMPLETED
            )
        )
        assertEquals(1, viaGeneralField.size)
        assertEquals(generalCategory.id, viaGeneralField[0].categoryId)
        assertEquals(generalCategory.id, viaGeneralField[0].generalCategoryId)
        assertNull(viaGeneralField[0].specificCategoryId)
        assertEquals("Moradia", viaGeneralField[0].generalCategory?.name)
        assertNull(viaGeneralField[0].specificCategory)
        assertEquals("Moradia", viaGeneralField[0].category?.name)

        val viaLegacyCategoryId = useCase.execute(
            userA.id,
            CreateTransactionRequest(
                title = "Outro Gasto Moradia",
                amount = 5000,
                date = LocalDate.of(2026, 11, 11),
                accountId = acc.id,
                type = TransactionType.EXPENSE,
                categoryId = generalCategory.id,
                status = TransactionStatus.COMPLETED
            )
        )
        assertEquals(generalCategory.id, viaLegacyCategoryId[0].categoryId)
        assertEquals(generalCategory.id, viaLegacyCategoryId[0].generalCategoryId)
        assertNull(viaLegacyCategoryId[0].specificCategoryId)
        assertEquals("Moradia", viaLegacyCategoryId[0].generalCategory?.name)
        assertNull(viaLegacyCategoryId[0].specificCategory)
    }

    @Test
    fun `should create transaction with GENERAL and SPECIFIC and reject mismatched, invalid level, or conflicting category fields`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val acc = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Conta Principal",
            startBalance = 500000,
            balance = 500000,
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
        val generalAlimentacao = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Alimentação",
            description = null,
            userId = userA.id,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#E4FF8F",
            icon = "🍽️",
            level = CategoryLevel.GENERAL
        )
        val specificMercado = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Supermercado",
            description = null,
            userId = userA.id,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#E4FF8F",
            icon = "🛒",
            level = CategoryLevel.SPECIFIC,
            parentId = generalAlimentacao.id
        )

        `when`(categoryRepository.findById(generalMoradia.id)).thenReturn(generalMoradia)
        `when`(categoryRepository.findById(specificAluguel.id)).thenReturn(specificAluguel)
        `when`(categoryRepository.findById(generalAlimentacao.id)).thenReturn(generalAlimentacao)
        `when`(categoryRepository.findById(specificMercado.id)).thenReturn(specificMercado)
        `when`(accountRepository.findById(acc.id)).thenReturn(acc)
        `when`(userRepository.findAll()).thenReturn(listOf(userA))
        `when`(transactionRepository.saveAll(anyNonNull())).thenAnswer { it.getArgument<List<TransactionEntity>>(0) }

        // Valid GENERAL + SPECIFIC
        val bothResult = useCase.execute(
            userA.id,
            CreateTransactionRequest(
                title = "Aluguel Nov",
                amount = 200000,
                date = LocalDate.of(2026, 11, 10),
                accountId = acc.id,
                type = TransactionType.EXPENSE,
                generalCategoryId = generalMoradia.id,
                specificCategoryId = specificAluguel.id,
                status = TransactionStatus.COMPLETED
            )
        )
        assertEquals(specificAluguel.id, bothResult[0].categoryId)
        assertEquals(generalMoradia.id, bothResult[0].generalCategoryId)
        assertEquals(specificAluguel.id, bothResult[0].specificCategoryId)
        assertEquals("Moradia", bothResult[0].generalCategory?.name)
        assertEquals("Aluguel", bothResult[0].specificCategory?.name)
        assertEquals("Moradia", bothResult[0].specificCategory?.parent?.name)
        assertEquals("Aluguel", bothResult[0].category?.name)

        // Valid specificCategoryId only (resolves general parent)
        val specificOnlyResult = useCase.execute(
            userA.id,
            CreateTransactionRequest(
                title = "Mercado",
                amount = 30000,
                date = LocalDate.of(2026, 11, 10),
                accountId = acc.id,
                type = TransactionType.EXPENSE,
                specificCategoryId = specificMercado.id,
                status = TransactionStatus.COMPLETED
            )
        )
        assertEquals(specificMercado.id, specificOnlyResult[0].categoryId)
        assertEquals(generalAlimentacao.id, specificOnlyResult[0].generalCategoryId)
        assertEquals(specificMercado.id, specificOnlyResult[0].specificCategoryId)

        // Reject SPECIFIC that does NOT belong to informed GENERAL
        assertThrows<InvalidTransactionException> {
            useCase.execute(
                userA.id,
                CreateTransactionRequest(
                    title = "Inconsistente",
                    amount = 5000,
                    date = LocalDate.of(2026, 11, 10),
                    accountId = acc.id,
                    type = TransactionType.EXPENSE,
                    generalCategoryId = generalMoradia.id,
                    specificCategoryId = specificMercado.id
                )
            )
        }

        // Reject SPECIFIC passed in generalCategoryId
        assertThrows<InvalidTransactionException> {
            useCase.execute(
                userA.id,
                CreateTransactionRequest(
                    title = "Específica como Geral",
                    amount = 5000,
                    date = LocalDate.of(2026, 11, 10),
                    accountId = acc.id,
                    type = TransactionType.EXPENSE,
                    generalCategoryId = specificAluguel.id
                )
            )
        }

        // Reject GENERAL passed in specificCategoryId
        assertThrows<InvalidTransactionException> {
            useCase.execute(
                userA.id,
                CreateTransactionRequest(
                    title = "Geral como Específica",
                    amount = 5000,
                    date = LocalDate.of(2026, 11, 10),
                    accountId = acc.id,
                    type = TransactionType.EXPENSE,
                    specificCategoryId = generalMoradia.id
                )
            )
        }

        // Reject conflict between specificCategoryId and categoryId
        assertThrows<InvalidTransactionException> {
            useCase.execute(
                userA.id,
                CreateTransactionRequest(
                    title = "Conflito",
                    amount = 5000,
                    date = LocalDate.of(2026, 11, 10),
                    accountId = acc.id,
                    type = TransactionType.EXPENSE,
                    generalCategoryId = generalMoradia.id,
                    specificCategoryId = specificAluguel.id,
                    categoryId = specificMercado.id
                )
            )
        }
    }

    @Test
    fun `should reject incompatible SPECIFIC category type for EXPENSE or INCOME but allow any type for TRANSFER`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val acc1 = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Corrente",
            startBalance = 50000,
            balance = 50000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )
        val acc2 = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Poupança",
            startBalance = 10000,
            balance = 10000,
            type = AccountType.SAVINGS,
            tax = 10,
            groupId = null
        )

        val generalIncome = CategoryEntity(
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
        val specificSalary = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Salário",
            description = null,
            userId = userA.id,
            groupId = null,
            type = CategoryType.INCOME,
            color = "#C6FF33",
            icon = "💼",
            level = CategoryLevel.SPECIFIC,
            parentId = generalIncome.id
        )

        `when`(categoryRepository.findById(specificSalary.id)).thenReturn(specificSalary)
        `when`(categoryRepository.findById(generalIncome.id)).thenReturn(generalIncome)
        `when`(accountRepository.findById(acc1.id)).thenReturn(acc1)
        `when`(accountRepository.findById(acc2.id)).thenReturn(acc2)
        `when`(userRepository.findAll()).thenReturn(listOf(userA))
        `when`(transactionRepository.saveAll(anyNonNull())).thenAnswer { it.getArgument<List<TransactionEntity>>(0) }

        // INCOME category on EXPENSE transaction -> InvalidTransactionException
        assertThrows<InvalidTransactionException> {
            useCase.execute(
                userA.id,
                CreateTransactionRequest(
                    title = "Compra",
                    amount = 5000,
                    date = LocalDate.of(2026, 11, 10),
                    accountId = acc1.id,
                    type = TransactionType.EXPENSE,
                    categoryId = specificSalary.id
                )
            )
        }

        // TRANSFER accepts any SPECIFIC category type and enriches parent
        val transferResult = useCase.execute(
            userA.id,
            CreateTransactionRequest(
                title = "Reserva",
                amount = 10000,
                date = LocalDate.of(2026, 11, 10),
                accountId = acc1.id,
                type = TransactionType.TRANSFER,
                destinationAccountId = acc2.id,
                categoryId = specificSalary.id,
                status = TransactionStatus.COMPLETED
            )
        )
        assertEquals(1, transferResult.size)
        assertEquals(specificSalary.id, transferResult[0].category?.id)
        assertEquals(CategoryLevel.SPECIFIC, transferResult[0].category?.level)
        assertEquals(generalIncome.id, transferResult[0].category?.parent?.id)
        assertEquals("Renda", transferResult[0].category?.parent?.name)
    }

    @Test
    fun `should reject group SPECIFIC category when transaction is on personal account`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val groupId = UUID.randomUUID()
        val personalAcc = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Conta Pessoal",
            startBalance = 50000,
            balance = 50000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )
        val groupGeneral = CategoryEntity(
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
        val groupSpecific = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Aluguel",
            description = null,
            userId = null,
            groupId = groupId,
            type = CategoryType.EXPENSE,
            color = "#7D39EB",
            icon = "🔑",
            level = CategoryLevel.SPECIFIC,
            parentId = groupGeneral.id
        )

        `when`(categoryRepository.findById(groupSpecific.id)).thenReturn(groupSpecific)
        `when`(categoryRepository.findById(groupGeneral.id)).thenReturn(groupGeneral)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(groupId, userA.id)).thenReturn(true)
        `when`(accountRepository.findById(personalAcc.id)).thenReturn(personalAcc)

        assertThrows<InvalidTransactionException> {
            useCase.execute(
                userA.id,
                CreateTransactionRequest(
                    title = "Aluguel",
                    amount = 100000,
                    date = LocalDate.of(2026, 11, 10),
                    accountId = personalAcc.id,
                    type = TransactionType.EXPENSE,
                    categoryId = groupSpecific.id
                )
            )
        }

        assertThrows<InvalidTransactionException> {
            useCase.execute(
                userA.id,
                CreateTransactionRequest(
                    title = "Moradia Grupo",
                    amount = 100000,
                    date = LocalDate.of(2026, 11, 10),
                    accountId = personalAcc.id,
                    type = TransactionType.EXPENSE,
                    generalCategoryId = groupGeneral.id
                )
            )
        }
    }

    @Test
    fun `should reject group SPECIFIC category when split mixes group account with personal or another group account`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val groupId = UUID.randomUUID()
        val otherGroupId = UUID.randomUUID()

        val groupAcc1 = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Conta Grupo 1",
            startBalance = 50000,
            balance = 50000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = groupId
        )
        val groupAcc2 = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Conta Grupo 2",
            startBalance = 50000,
            balance = 50000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = groupId
        )
        val personalAcc = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Conta Pessoal",
            startBalance = 50000,
            balance = 50000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )
        val otherGroupAcc = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Conta Outro Grupo",
            startBalance = 50000,
            balance = 50000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = otherGroupId
        )

        val groupGeneral = CategoryEntity(
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
        val groupSpecific = CategoryEntity(
            id = UUID.randomUUID(),
            name = "Condomínio",
            description = null,
            userId = null,
            groupId = groupId,
            type = CategoryType.EXPENSE,
            color = "#7D39EB",
            icon = "🏢",
            level = CategoryLevel.SPECIFIC,
            parentId = groupGeneral.id
        )

        `when`(categoryRepository.findById(groupSpecific.id)).thenReturn(groupSpecific)
        `when`(categoryRepository.findById(groupGeneral.id)).thenReturn(groupGeneral)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(groupId, userA.id)).thenReturn(true)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(otherGroupId, userA.id)).thenReturn(true)
        `when`(accountRepository.findById(groupAcc1.id)).thenReturn(groupAcc1)
        `when`(accountRepository.findById(groupAcc2.id)).thenReturn(groupAcc2)
        `when`(accountRepository.findById(personalAcc.id)).thenReturn(personalAcc)
        `when`(accountRepository.findById(otherGroupAcc.id)).thenReturn(otherGroupAcc)
        `when`(userRepository.findAll()).thenReturn(listOf(userA))
        `when`(transactionRepository.saveAll(anyNonNull())).thenAnswer { it.getArgument<List<TransactionEntity>>(0) }

        // Mix group + personal -> rejected
        assertThrows<InvalidTransactionException> {
            useCase.execute(
                userA.id,
                CreateTransactionRequest(
                    title = "Condomínio",
                    amount = 20000,
                    date = LocalDate.of(2026, 11, 10),
                    accountId = groupAcc1.id,
                    type = TransactionType.EXPENSE,
                    categoryId = groupSpecific.id,
                    splits = listOf(
                        AccountSplitRequest(groupAcc1.id, 10000),
                        AccountSplitRequest(personalAcc.id, 10000)
                    )
                )
            )
        }

        // Mix group + another group -> rejected
        assertThrows<InvalidTransactionException> {
            useCase.execute(
                userA.id,
                CreateTransactionRequest(
                    title = "Condomínio",
                    amount = 20000,
                    date = LocalDate.of(2026, 11, 10),
                    accountId = groupAcc1.id,
                    type = TransactionType.EXPENSE,
                    categoryId = groupSpecific.id,
                    splits = listOf(
                        AccountSplitRequest(groupAcc1.id, 10000),
                        AccountSplitRequest(otherGroupAcc.id, 10000)
                    )
                )
            )
        }

        // All accounts in same group -> allowed
        val validResult = useCase.execute(
            userA.id,
            CreateTransactionRequest(
                title = "Condomínio",
                amount = 20000,
                date = LocalDate.of(2026, 11, 10),
                accountId = groupAcc1.id,
                type = TransactionType.EXPENSE,
                categoryId = groupSpecific.id,
                splits = listOf(
                    AccountSplitRequest(groupAcc1.id, 10000),
                    AccountSplitRequest(groupAcc2.id, 10000)
                )
            )
        )
        assertEquals(2, validResult.size)
        assertEquals(groupSpecific.id, validResult[0].categoryId)
    }

    @Test
    fun `should allow authorized joint account member in splits and reject unauthorized member or generatesDebt`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val userB = UserEntity(Name("Maria"), Name("Silva"), Email("maria@finance.com"), PasswordHash("hash"))
        val groupId = UUID.randomUUID()

        val accA = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Conta Arthur",
            startBalance = 50000,
            balance = 50000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = groupId
        )
        val jointAcc = AccountEntity.createJoint(
            groupId = groupId,
            name = "Conjunta Secreta",
            startBalance = 50000,
            type = AccountType.CHECKING
        )

        `when`(accountRepository.findById(accA.id)).thenReturn(accA)
        `when`(accountRepository.findById(jointAcc.id)).thenReturn(jointAcc)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(groupId, userA.id)).thenReturn(true)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(groupId, userB.id)).thenReturn(true)
        `when`(jointAccountMemberRepository.existsByAccountIdAndUserId(jointAcc.id, userA.id)).thenReturn(true)
        `when`(jointAccountMemberRepository.existsByAccountIdAndUserId(jointAcc.id, userB.id)).thenReturn(false)
        `when`(userRepository.findAll()).thenReturn(listOf(userA, userB))
        `when`(transactionRepository.saveAll(anyNonNull())).thenAnswer { it.getArgument<List<TransactionEntity>>(0) }

        // Split between individual and authorized joint account uses userA.id when splitAccount.ownerId is null
        val splitResult = useCase.execute(
            userA.id,
            CreateTransactionRequest(
                title = "Compra Split",
                amount = 10000,
                date = LocalDate.of(2026, 11, 10),
                accountId = accA.id,
                type = TransactionType.EXPENSE,
                status = TransactionStatus.COMPLETED,
                splits = listOf(
                    AccountSplitRequest(accA.id, 6000),
                    AccountSplitRequest(jointAcc.id, 4000)
                )
            )
        )
        assertEquals(2, splitResult.size)
        assertEquals(userA.id, splitResult[1].responsibleId)

        // userB belongs to groupId, NOT in joint_account_members -> AccessDeniedException without leaking name
        val ex = assertThrows<AccessDeniedException> {
            useCase.execute(
                userB.id,
                CreateTransactionRequest(
                    title = "Invasão",
                    amount = 1000,
                    date = LocalDate.of(2026, 11, 10),
                    accountId = jointAcc.id,
                    type = TransactionType.EXPENSE
                )
            )
        }
        assertFalse(ex.message!!.contains("Conjunta Secreta"))

        // Transfer with generatesDebt = true involving joint account is rejected
        assertThrows<InvalidTransactionException> {
            useCase.execute(
                userA.id,
                CreateTransactionRequest(
                    title = "Aporte Dívida",
                    amount = 5000,
                    date = LocalDate.of(2026, 11, 10),
                    accountId = accA.id,
                    type = TransactionType.TRANSFER,
                    destinationAccountId = jointAcc.id,
                    generatesDebt = true
                )
            )
        }
    }

    @Test
    fun `should reject TRANSFER when source is another group member individual account, even within same group`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val userB = UserEntity(Name("Maria"), Name("Silva"), Email("maria@finance.com"), PasswordHash("hash"))
        val groupId = UUID.randomUUID()

        val accA = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Conta Arthur",
            startBalance = 50000,
            balance = 50000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = groupId
        )
        val accB = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userB.id,
            name = "Conta Privada Maria",
            startBalance = 40000,
            balance = 40000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = groupId
        )
        val jointAcc = AccountEntity.createJoint(
            groupId = groupId,
            name = "Conjunta Casa",
            startBalance = 10000,
            type = AccountType.CHECKING
        )

        `when`(accountRepository.findById(accA.id)).thenReturn(accA)
        `when`(accountRepository.findById(accB.id)).thenReturn(accB)
        `when`(accountRepository.findById(jointAcc.id)).thenReturn(jointAcc)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(groupId, userA.id)).thenReturn(true)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(groupId, userB.id)).thenReturn(true)
        `when`(jointAccountMemberRepository.existsByAccountIdAndUserId(jointAcc.id, userA.id)).thenReturn(true)
        `when`(jointAccountMemberRepository.existsByAccountIdAndUserId(jointAcc.id, userB.id)).thenReturn(true)

        // userA tentando fazer aporte da conta individual de userB (no mesmo grupo) para a conta conjunta -> bloqueado
        val exToJoint = assertThrows<AccessDeniedException> {
            useCase.execute(
                userA.id,
                CreateTransactionRequest(
                    title = "Aporte indevido",
                    amount = 5000,
                    date = LocalDate.of(2026, 11, 10),
                    accountId = accB.id,
                    type = TransactionType.TRANSFER,
                    destinationAccountId = jointAcc.id,
                    status = TransactionStatus.COMPLETED,
                    generatesDebt = false
                )
            )
        }
        assertFalse(exToJoint.message!!.contains("Conta Privada Maria"))

        // userA tentando transferir da conta individual de userB para sua própria conta -> bloqueado
        assertThrows<AccessDeniedException> {
            useCase.execute(
                userA.id,
                CreateTransactionRequest(
                    title = "Transferência indevida",
                    amount = 5000,
                    date = LocalDate.of(2026, 11, 10),
                    accountId = accB.id,
                    type = TransactionType.TRANSFER,
                    destinationAccountId = accA.id,
                    status = TransactionStatus.COMPLETED,
                    generatesDebt = false
                )
            )
        }

        assertEquals(40000, accB.balance)
        assertEquals(10000, jointAcc.balance)
        assertEquals(50000, accA.balance)
        verify(transactionRepository, never()).saveAll(anyNonNull())
    }

    @Test
    fun `should allow TRANSFER from own individual account to joint or group member account, and from accessible joint account`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val userB = UserEntity(Name("Maria"), Name("Silva"), Email("maria@finance.com"), PasswordHash("hash"))
        val groupId = UUID.randomUUID()

        val accA = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userA.id,
            name = "Conta Arthur",
            startBalance = 50000,
            balance = 50000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = groupId
        )
        val accB = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userB.id,
            name = "Conta Maria",
            startBalance = 20000,
            balance = 20000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = groupId
        )
        val jointAcc = AccountEntity.createJoint(
            groupId = groupId,
            name = "Conjunta Casa",
            startBalance = 10000,
            type = AccountType.CHECKING
        )

        `when`(accountRepository.findById(accA.id)).thenReturn(accA)
        `when`(accountRepository.findById(accB.id)).thenReturn(accB)
        `when`(accountRepository.findById(jointAcc.id)).thenReturn(jointAcc)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(groupId, userA.id)).thenReturn(true)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(groupId, userB.id)).thenReturn(true)
        `when`(jointAccountMemberRepository.existsByAccountIdAndUserId(jointAcc.id, userA.id)).thenReturn(true)
        `when`(jointAccountMemberRepository.existsByAccountIdAndUserId(jointAcc.id, userB.id)).thenReturn(false)
        `when`(userRepository.findAll()).thenReturn(listOf(userA, userB))
        `when`(transactionRepository.saveAll(anyNonNull())).thenAnswer { it.getArgument<List<TransactionEntity>>(0) }

        // 1. Própria conta -> conta conjunta acessível
        val toJoint = useCase.execute(
            userA.id,
            CreateTransactionRequest(
                title = "Aporte Casa",
                amount = 10000,
                date = LocalDate.of(2026, 11, 10),
                accountId = accA.id,
                type = TransactionType.TRANSFER,
                destinationAccountId = jointAcc.id,
                status = TransactionStatus.COMPLETED
            )
        )
        assertEquals(1, toJoint.size)
        assertEquals(userA.id, toJoint[0].responsibleId)
        assertEquals(40000, accA.balance)
        assertEquals(20000, jointAcc.balance)

        // 2. Própria conta -> conta individual de outro integrante do grupo
        val toColleague = useCase.execute(
            userA.id,
            CreateTransactionRequest(
                title = "PIX Maria",
                amount = 5000,
                date = LocalDate.of(2026, 11, 11),
                accountId = accA.id,
                type = TransactionType.TRANSFER,
                destinationAccountId = accB.id,
                status = TransactionStatus.COMPLETED
            )
        )
        assertEquals(1, toColleague.size)
        assertEquals(35000, accA.balance)
        assertEquals(25000, accB.balance)

        // 3. Conta conjunta onde userA tem acesso -> própria conta individual
        val fromJoint = useCase.execute(
            userA.id,
            CreateTransactionRequest(
                title = "Reembolso Conjunta",
                amount = 4000,
                date = LocalDate.of(2026, 11, 12),
                accountId = jointAcc.id,
                type = TransactionType.TRANSFER,
                destinationAccountId = accA.id,
                status = TransactionStatus.COMPLETED
            )
        )
        assertEquals(1, fromJoint.size)
        assertEquals(16000, jointAcc.balance)
        assertEquals(39000, accA.balance)

        // 4. userB sem acesso à conta conjunta não pode transferir a partir dela
        assertThrows<AccessDeniedException> {
            useCase.execute(
                userB.id,
                CreateTransactionRequest(
                    title = "Saque sem acesso",
                    amount = 1000,
                    date = LocalDate.of(2026, 11, 12),
                    accountId = jointAcc.id,
                    type = TransactionType.TRANSFER,
                    destinationAccountId = accB.id,
                    status = TransactionStatus.COMPLETED
                )
            )
        }
    }
}
