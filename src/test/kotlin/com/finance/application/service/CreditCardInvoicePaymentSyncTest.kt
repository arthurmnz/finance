package com.finance.application.service

import com.finance.application.dto.credit_card.PayInvoiceRequest
import com.finance.application.dto.transaction.UpdateTransactionRequest
import com.finance.application.use_case.transaction.DeleteTransactionUseCase
import com.finance.application.use_case.transaction.ToggleTransactionStatusUseCase
import com.finance.application.use_case.transaction.UpdateTransactionUseCase
import com.finance.domain.entity.AccountEntity
import com.finance.domain.entity.CreditCardEntity
import com.finance.domain.entity.CreditCardInvoiceEntity
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.entity.UserEntity
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import com.finance.domain.enum.AccountType
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.CreditCardGroupRequestRepository
import com.finance.domain.repository.CreditCardInvoiceRepository
import com.finance.domain.repository.CreditCardRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyList
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Suppress("UNCHECKED_CAST")
private fun <T> anyNonNull(): T {
    org.mockito.Mockito.any<T>()
    return null as T
}

class CreditCardInvoicePaymentSyncTest {

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var creditCardRepository: CreditCardRepository
    private lateinit var invoiceRepository: CreditCardInvoiceRepository
    private lateinit var userRepository: UserRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var groupMemberRepository: GroupMemberRepository
    private lateinit var groupService: GroupService
    private lateinit var groupRequestRepository: CreditCardGroupRequestRepository

    private lateinit var deleteTransactionUseCase: DeleteTransactionUseCase
    private lateinit var updateTransactionUseCase: UpdateTransactionUseCase
    private lateinit var toggleTransactionStatusUseCase: ToggleTransactionStatusUseCase
    private lateinit var creditCardService: CreditCardService

    private val userId = UUID.randomUUID()
    private val accountId = UUID.randomUUID()
    private val cardId = UUID.randomUUID()
    private val invoiceId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        transactionRepository = mock(TransactionRepository::class.java)
        accountRepository = mock(AccountRepository::class.java)
        creditCardRepository = mock(CreditCardRepository::class.java)
        invoiceRepository = mock(CreditCardInvoiceRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        categoryRepository = mock(CategoryRepository::class.java)
        groupMemberRepository = mock(GroupMemberRepository::class.java)
        groupService = mock(GroupService::class.java)
        groupRequestRepository = mock(CreditCardGroupRequestRepository::class.java)

        deleteTransactionUseCase = DeleteTransactionUseCase(
            transactionRepository,
            accountRepository,
            invoiceRepository,
            creditCardRepository
        )

        updateTransactionUseCase = UpdateTransactionUseCase(
            transactionRepository,
            accountRepository,
            userRepository,
            categoryRepository,
            groupMemberRepository,
            invoiceRepository,
            creditCardRepository
        )

        toggleTransactionStatusUseCase = ToggleTransactionStatusUseCase(
            transactionRepository,
            accountRepository,
            userRepository,
            categoryRepository,
            invoiceRepository,
            creditCardRepository
        )

        creditCardService = CreditCardService(
            creditCardRepository,
            invoiceRepository,
            groupRequestRepository,
            accountRepository,
            transactionRepository,
            groupMemberRepository,
            groupService,
            userRepository,
            categoryRepository
        )
    }

    @Test
    fun `should decrease invoice paidAmount and refund account balance when payment transaction is deleted`() {
        val account = AccountEntity(
            id = accountId,
            ownerId = userId,
            name = "Conta Nubank",
            startBalance = 100000,
            balance = 70000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )

        val invoice = CreditCardInvoiceEntity(
            id = invoiceId,
            creditCardId = cardId,
            month = 10,
            year = 2026,
            paidAmount = 30000
        )

        val paymentTx = TransactionEntity(
            title = "Pagamento fatura Roxinho (10/2026)",
            amount = 30000,
            date = LocalDate.of(2026, 10, 20),
            responsibleId = userId,
            accountId = accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            creditCardInvoiceId = invoiceId
        )

        `when`(transactionRepository.findById(paymentTx.id)).thenReturn(paymentTx)
        `when`(accountRepository.findById(accountId)).thenReturn(account)
        `when`(invoiceRepository.findById(invoiceId)).thenReturn(invoice)

        deleteTransactionUseCase.execute(userId, paymentTx.id)

        // Saldo da conta estornado (+30000)
        assertEquals(100000, account.balance)
        // Pagamento da fatura reduzido
        assertEquals(0, invoice.paidAmount)
        verify(invoiceRepository).save(invoice)
        verify(accountRepository).save(account)
        verify(transactionRepository).deleteAll(listOf(paymentTx.id))
    }

    @Test
    fun `should adjust invoice paidAmount when payment transaction is updated`() {
        val account = AccountEntity(
            id = accountId,
            ownerId = userId,
            name = "Conta Nubank",
            startBalance = 100000,
            balance = 80000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )

        val invoice = CreditCardInvoiceEntity(
            id = invoiceId,
            creditCardId = cardId,
            month = 10,
            year = 2026,
            paidAmount = 20000
        )

        val paymentTx = TransactionEntity(
            title = "Pagamento fatura Roxinho (10/2026)",
            amount = 20000,
            date = LocalDate.of(2026, 10, 20),
            responsibleId = userId,
            accountId = accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            creditCardInvoiceId = invoiceId
        )

        val user = UserEntity(
            id = userId,
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur@test.com"),
            passwordHash = PasswordHash("hash")
        )

        `when`(transactionRepository.findById(paymentTx.id)).thenReturn(paymentTx)
        `when`(accountRepository.findById(accountId)).thenReturn(account)
        `when`(invoiceRepository.findById(invoiceId)).thenReturn(invoice)
        `when`(userRepository.findById(userId)).thenReturn(user)
        `when`(transactionRepository.saveAll(anyList())).thenAnswer { it.arguments[0] }

        // Aumentando o valor pago de 20000 para 35000
        val request = UpdateTransactionRequest(
            title = "Pagamento fatura Roxinho (10/2026)",
            amount = 35000,
            date = LocalDate.of(2026, 10, 20),
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            updateAllFuture = false
        )

        updateTransactionUseCase.execute(userId, paymentTx.id, request)

        assertEquals(35000, invoice.paidAmount)
        verify(invoiceRepository).save(invoice)
    }

    @Test
    fun `should adjust invoice paidAmount on toggleTransactionStatus`() {
        val account = AccountEntity(
            id = accountId,
            ownerId = userId,
            name = "Conta Nubank",
            startBalance = 100000,
            balance = 75000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )

        val invoice = CreditCardInvoiceEntity(
            id = invoiceId,
            creditCardId = cardId,
            month = 10,
            year = 2026,
            paidAmount = 25000
        )

        val paymentTx = TransactionEntity(
            title = "Pagamento fatura Roxinho (10/2026)",
            amount = 25000,
            date = LocalDate.of(2026, 10, 20),
            responsibleId = userId,
            accountId = accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            creditCardInvoiceId = invoiceId
        )

        val user = UserEntity(
            id = userId,
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur@test.com"),
            passwordHash = PasswordHash("hash")
        )

        `when`(transactionRepository.findById(paymentTx.id)).thenReturn(paymentTx)
        `when`(accountRepository.findById(accountId)).thenReturn(account)
        `when`(invoiceRepository.findById(invoiceId)).thenReturn(invoice)
        `when`(userRepository.findById(userId)).thenReturn(user)
        `when`(transactionRepository.save(paymentTx)).thenReturn(paymentTx)

        // Alternar de COMPLETED para PENDING -> deve subtrair da fatura
        toggleTransactionStatusUseCase.execute(userId, paymentTx.id)

        assertEquals(0, invoice.paidAmount)
        verify(invoiceRepository).save(invoice)
    }

    @Test
    fun `should auto heal orphan invoice paidAmount in CreditCardService getInvoice when payment transaction was deleted`() {
        val card = CreditCardEntity(
            id = cardId,
            ownerId = userId,
            accountId = accountId,
            name = "Roxinho",
            limitAmount = 500000,
            closingDay = 10,
            dueDay = 17
        )

        // Fatura no banco com paidAmount = 50000 órfão
        val invoice = CreditCardInvoiceEntity(
            id = invoiceId,
            creditCardId = cardId,
            month = 10,
            year = 2026,
            paidAmount = 50000
        )

        val user = UserEntity(
            id = userId,
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur@test.com"),
            passwordHash = PasswordHash("hash")
        )

        `when`(creditCardRepository.findById(cardId)).thenReturn(card)
        `when`(invoiceRepository.findByCardAndPeriod(cardId, 10, 2026)).thenReturn(invoice)
        `when`(invoiceRepository.save(invoice)).thenReturn(invoice)
        `when`(transactionRepository.findByCreditCardId(cardId)).thenReturn(emptyList())
        `when`(transactionRepository.findByCreditCardInvoiceId(invoiceId)).thenReturn(emptyList())
        `when`(transactionRepository.findByAccountId(accountId)).thenReturn(emptyList())
        `when`(userRepository.findAll()).thenReturn(listOf(user))

        val response = creditCardService.getInvoice(cardId, 10, 2026, userId)

        // O auto-healing curou a discrepância: paidAmount deve ser 0!
        assertEquals(0, response.paidAmount)
        assertEquals(0, invoice.paidAmount)
        verify(invoiceRepository).save(invoice)
    }

    @Test
    fun `should pay invoice and link payment transaction to invoice`() {
        val card = CreditCardEntity(
            id = cardId,
            ownerId = userId,
            accountId = accountId,
            name = "Roxinho",
            limitAmount = 500000,
            closingDay = 10,
            dueDay = 17
        )

        val account = AccountEntity(
            id = accountId,
            ownerId = userId,
            name = "Conta Nubank",
            startBalance = 100000,
            balance = 100000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )

        val invoice = CreditCardInvoiceEntity(
            id = invoiceId,
            creditCardId = cardId,
            month = 10,
            year = 2026,
            paidAmount = 0
        )

        val user = UserEntity(
            id = userId,
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur@test.com"),
            passwordHash = PasswordHash("hash")
        )

        val savedTxs = mutableListOf<TransactionEntity>()
        `when`(creditCardRepository.findById(cardId)).thenReturn(card)
        `when`(accountRepository.findById(accountId)).thenReturn(account)
        `when`(invoiceRepository.findByCardAndPeriod(cardId, 10, 2026)).thenReturn(invoice)
        `when`(invoiceRepository.save(invoice)).thenReturn(invoice)
        `when`(userRepository.findAll()).thenReturn(listOf(user))
        `when`(transactionRepository.save(anyNonNull())).thenAnswer {
            val tx = it.arguments[0] as TransactionEntity
            savedTxs.add(tx)
            tx
        }
        `when`(transactionRepository.findByCreditCardInvoiceId(invoiceId)).thenAnswer { savedTxs.toList() }
        `when`(transactionRepository.findByCreditCardId(cardId)).thenReturn(emptyList())
        `when`(transactionRepository.findByAccountId(accountId)).thenAnswer { savedTxs.toList() }

        val payRequest = PayInvoiceRequest(
            amount = 40000,
            accountId = accountId,
            date = LocalDate.of(2026, 10, 15),
            rollRemainingToNextMonth = false
        )

        val response = creditCardService.payInvoice(cardId, 10, 2026, payRequest, userId)

        assertEquals(60000, account.balance)
        assertEquals(40000, response.paidAmount)
        assertEquals(40000, invoice.paidAmount)
        assertEquals(1, response.payments.size)
        assertEquals(40000, response.payments[0].amount)
        assertEquals(invoiceId, response.payments[0].creditCardInvoiceId)
        verify(accountRepository).save(account)
    }
}
