package com.finance.application.use_case.transaction

import com.finance.application.dto.transaction.AccountSplitRequest
import com.finance.application.dto.transaction.CreateTransactionRequest
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
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var useCase: CreateTransactionUseCase

    @BeforeEach
    fun setup() {
        transactionRepository = mock(TransactionRepository::class.java)
        accountRepository = mock(AccountRepository::class.java)
        creditCardService = mock(CreditCardService::class.java)
        userRepository = mock(UserRepository::class.java)
        groupMemberRepository = mock(GroupMemberRepository::class.java)
        categoryRepository = mock(CategoryRepository::class.java)

        useCase = CreateTransactionUseCase(
            transactionRepository = transactionRepository,
            accountRepository = accountRepository,
            creditCardService = creditCardService,
            userRepository = userRepository,
            groupMemberRepository = groupMemberRepository,
            categoryRepository = categoryRepository
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
    fun `should reject linking transaction to GENERAL category`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val accountId = UUID.randomUUID()
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

        assertThrows<InvalidTransactionException> {
            useCase.execute(
                userA.id,
                CreateTransactionRequest(
                    title = "Aluguel",
                    amount = 200000,
                    date = LocalDate.of(2026, 11, 10),
                    accountId = accountId,
                    type = TransactionType.EXPENSE,
                    categoryId = generalCategory.id
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
}
