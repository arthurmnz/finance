package com.finance.application.service

import com.finance.application.dto.credit_card.CreateCreditCardRequest
import com.finance.application.dto.credit_card.PayInvoiceRequest
import com.finance.application.dto.credit_card.UpdateCreditCardRequest
import com.finance.application.dto.credit_card.UpdateInvoiceRevolvingRequest
import com.finance.application.dto.transaction.UpdateTransactionRequest
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.InvalidTransactionException
import com.finance.domain.exception.UnauthorizedException
import org.junit.jupiter.api.assertThrows
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
import com.finance.domain.enum.RecurrenceFrequency
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.CreditCardGroupRequestRepository
import com.finance.domain.repository.CreditCardInvoiceRepository
import com.finance.domain.repository.CreditCardRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.JointAccountMemberRepository
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
    private lateinit var jointAccountMemberRepository: JointAccountMemberRepository
    private lateinit var groupService: GroupService
    private lateinit var groupRequestRepository: CreditCardGroupRequestRepository
    private lateinit var accountAccessService: AccountAccessService

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
        jointAccountMemberRepository = mock(JointAccountMemberRepository::class.java)
        groupService = mock(GroupService::class.java)
        groupRequestRepository = mock(CreditCardGroupRequestRepository::class.java)
        accountAccessService = AccountAccessService(groupMemberRepository, jointAccountMemberRepository)

        deleteTransactionUseCase = DeleteTransactionUseCase(
            transactionRepository,
            accountRepository,
            invoiceRepository,
            creditCardRepository,
            accountAccessService
        )

        updateTransactionUseCase = UpdateTransactionUseCase(
            transactionRepository,
            accountRepository,
            userRepository,
            categoryRepository,
            groupMemberRepository,
            invoiceRepository,
            creditCardRepository,
            accountAccessService
        )

        toggleTransactionStatusUseCase = ToggleTransactionStatusUseCase(
            transactionRepository,
            accountRepository,
            userRepository,
            categoryRepository,
            invoiceRepository,
            creditCardRepository,
            accountAccessService
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
            categoryRepository,
            accountAccessService
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

    @Test
    fun `should reject linking credit card to joint account and validate account access on payInvoice`() {
        val groupId = UUID.randomUUID()
        val jointAcc = AccountEntity.createJoint(groupId, "Conjunta", 50000, AccountType.CHECKING)
        val personalAcc = AccountEntity(
            id = accountId,
            ownerId = userId,
            name = "Pessoal",
            startBalance = 50000,
            balance = 50000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = groupId
        )
        val card = CreditCardEntity(
            id = cardId,
            ownerId = userId,
            accountId = accountId,
            name = "Roxinho",
            limitAmount = 500000,
            closingDay = 10,
            dueDay = 17
        )

        `when`(accountRepository.findById(jointAcc.id)).thenReturn(jointAcc)
        `when`(accountRepository.findById(personalAcc.id)).thenReturn(personalAcc)
        `when`(creditCardRepository.findById(cardId)).thenReturn(card)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)).thenReturn(true)
        `when`(jointAccountMemberRepository.existsByAccountIdAndUserId(jointAcc.id, userId)).thenReturn(false)

        assertThrows<AccessDeniedException> {
            creditCardService.create(
                CreateCreditCardRequest("Card Conjunta", jointAcc.id, 100000, 10, 17),
                userId
            )
        }

        assertThrows<AccessDeniedException> {
            creditCardService.update(
                cardId,
                UpdateCreditCardRequest("Card Conjunta", jointAcc.id, 100000, 10, 17),
                userId
            )
        }

        assertThrows<AccessDeniedException> {
            creditCardService.payInvoice(
                cardId,
                10,
                2026,
                PayInvoiceRequest(amount = 1000, accountId = jointAcc.id),
                userId
            )
        }
    }

    @Test
    fun `should reject invalid invoice month and year in getInvoice, payInvoice and updateInvoiceRevolving`() {
        assertThrows<InvalidTransactionException> {
            creditCardService.getInvoice(cardId, 13, 2026, userId)
        }
        assertThrows<InvalidTransactionException> {
            creditCardService.getInvoice(cardId, 1, 9999, userId)
        }
        assertThrows<InvalidTransactionException> {
            creditCardService.payInvoice(cardId, 0, 2026, PayInvoiceRequest(amount = 1000), userId)
        }
        assertThrows<InvalidTransactionException> {
            creditCardService.payInvoice(cardId, 1, 1899, PayInvoiceRequest(amount = 1000), userId)
        }
        assertThrows<InvalidTransactionException> {
            creditCardService.updateInvoiceRevolving(
                cardId,
                13,
                2026,
                UpdateInvoiceRevolvingRequest(revolvingInterestAmount = 100),
                userId
            )
        }
        assertThrows<InvalidTransactionException> {
            creditCardService.updateInvoiceRevolving(
                cardId,
                10,
                2101,
                UpdateInvoiceRevolvingRequest(revolvingInterestAmount = 100),
                userId
            )
        }
    }

    @Test
    fun `should compute usedLimit 19994 and individual invoice totals 5000, 4998, 4998, 4998 for 4x installment purchase`() {
        val card = CreditCardEntity(
            id = cardId,
            ownerId = userId,
            accountId = accountId,
            name = "Roxinho",
            limitAmount = 500000,
            closingDay = 10,
            dueDay = 17
        )
        val user = UserEntity(
            id = userId,
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur@test.com"),
            passwordHash = PasswordHash("hash")
        )
        val groupId = UUID.randomUUID()
        val p1 = TransactionEntity(
            title = "Compra 4x",
            amount = 5000,
            date = LocalDate.of(2026, 11, 5),
            responsibleId = userId,
            accountId = accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            recurrenceGroupId = groupId,
            recurrenceFrequency = RecurrenceFrequency.MONTHLY,
            recurrenceInterval = 1,
            creditCardId = cardId,
            invoiceMonth = 11,
            invoiceYear = 2026,
            installmentNumber = 1,
            totalInstallments = 4
        )
        val p2 = TransactionEntity(
            title = "Compra 4x",
            amount = 4998,
            date = LocalDate.of(2026, 11, 10),
            responsibleId = userId,
            accountId = accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            recurrenceGroupId = groupId,
            recurrenceFrequency = RecurrenceFrequency.MONTHLY,
            recurrenceInterval = 1,
            creditCardId = cardId,
            invoiceMonth = 12,
            invoiceYear = 2026,
            installmentNumber = 2,
            totalInstallments = 4
        )
        val p3 = TransactionEntity(
            title = "Compra 4x",
            amount = 4998,
            date = LocalDate.of(2026, 12, 10),
            responsibleId = userId,
            accountId = accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            recurrenceGroupId = groupId,
            recurrenceFrequency = RecurrenceFrequency.MONTHLY,
            recurrenceInterval = 1,
            creditCardId = cardId,
            invoiceMonth = 1,
            invoiceYear = 2027,
            installmentNumber = 3,
            totalInstallments = 4
        )
        val p4 = TransactionEntity(
            title = "Compra 4x",
            amount = 4998,
            date = LocalDate.of(2027, 1, 10),
            responsibleId = userId,
            accountId = accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            recurrenceGroupId = groupId,
            recurrenceFrequency = RecurrenceFrequency.MONTHLY,
            recurrenceInterval = 1,
            creditCardId = cardId,
            invoiceMonth = 2,
            invoiceYear = 2027,
            installmentNumber = 4,
            totalInstallments = 4
        )

        `when`(creditCardRepository.findAllByOwnerId(userId)).thenReturn(listOf(card))
        `when`(creditCardRepository.findById(cardId)).thenReturn(card)
        `when`(groupMemberRepository.findAllByUserId(userId)).thenReturn(emptyList())
        `when`(creditCardRepository.findAllByGroupIdIn(emptyList())).thenReturn(emptyList())
        `when`(transactionRepository.findByCreditCardId(cardId)).thenReturn(listOf(p1, p2, p3, p4))
        `when`(invoiceRepository.findAllByCreditCardId(cardId)).thenReturn(emptyList())
        `when`(transactionRepository.findByAccountId(accountId)).thenReturn(emptyList())
        `when`(userRepository.findAll()).thenReturn(listOf(user))

        val cards = creditCardService.listAccessibleCards(userId)
        assertEquals(1, cards.size)
        assertEquals(19994, cards[0].usedLimit)
        assertEquals(500000 - 19994, cards[0].availableLimit)

        val inv1 = creditCardService.getInvoice(cardId, 11, 2026, userId)
        val inv2 = creditCardService.getInvoice(cardId, 12, 2026, userId)
        val inv3 = creditCardService.getInvoice(cardId, 1, 2027, userId)
        val inv4 = creditCardService.getInvoice(cardId, 2, 2027, userId)

        assertEquals(5000, inv1.purchasesAmount)
        assertEquals(4998, inv2.purchasesAmount)
        assertEquals(4998, inv3.purchasesAmount)
        assertEquals(4998, inv4.purchasesAmount)
        assertEquals(19994, inv1.totalAmount + inv2.totalAmount + inv3.totalAmount + inv4.totalAmount)
    }

    @Test
    fun `should prevent 32-bit Int wrap-around in CreditCardService and enforce mutation access on future deletes`() {
        val card = CreditCardEntity(
            id = cardId,
            ownerId = userId,
            accountId = accountId,
            name = "Black",
            limitAmount = 2_000_000_000,
            closingDay = 10,
            dueDay = 17
        )
        val user = UserEntity(
            id = userId,
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur@test.com"),
            passwordHash = PasswordHash("hash")
        )
        val bigTx1 = TransactionEntity(
            title = "Compra 1",
            amount = 1_500_000_000,
            date = LocalDate.of(2026, 11, 5),
            responsibleId = userId,
            accountId = accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            creditCardId = cardId,
            invoiceMonth = 11,
            invoiceYear = 2026
        )
        val bigTx2 = TransactionEntity(
            title = "Compra 2",
            amount = 1_500_000_000,
            date = LocalDate.of(2026, 11, 6),
            responsibleId = userId,
            accountId = accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            creditCardId = cardId,
            invoiceMonth = 11,
            invoiceYear = 2026
        )

        `when`(creditCardRepository.findAllByOwnerId(userId)).thenReturn(listOf(card))
        `when`(creditCardRepository.findById(cardId)).thenReturn(card)
        `when`(groupMemberRepository.findAllByUserId(userId)).thenReturn(emptyList())
        `when`(creditCardRepository.findAllByGroupIdIn(emptyList())).thenReturn(emptyList())
        `when`(transactionRepository.findByCreditCardId(cardId)).thenReturn(listOf(bigTx1, bigTx2))
        `when`(invoiceRepository.findAllByCreditCardId(cardId)).thenReturn(emptyList())
        `when`(transactionRepository.findByAccountId(accountId)).thenReturn(emptyList())
        `when`(userRepository.findAll()).thenReturn(listOf(user))

        // 1_500_000_000 + 1_500_000_000 = 3_000_000_000L (> Int.MAX_VALUE); não pode sofrer wrap-around para negativo/0
        val cardView = creditCardService.listAccessibleCards(userId).first()
        assertEquals(Int.MAX_VALUE, cardView.usedLimit)
        assertEquals(0, cardView.availableLimit)

        val invoiceView = creditCardService.getInvoice(cardId, 11, 2026, userId)
        assertEquals(Int.MAX_VALUE, invoiceView.purchasesAmount)
        assertEquals(Int.MAX_VALUE, invoiceView.totalAmount)
        assertEquals(Int.MAX_VALUE, invoiceView.remainingAmount)
        assertEquals("CLOSED", invoiceView.status)

        // DeleteTransactionUseCase com deleteAllFuture = true deve checar permissão em cada transação futura
        val groupId = UUID.randomUUID()
        val otherUserId = UUID.randomUUID()
        val ownFirst = TransactionEntity(
            title = "Parcela 1",
            amount = 5000,
            date = LocalDate.of(2026, 11, 5),
            responsibleId = userId,
            accountId = accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            recurrenceGroupId = groupId,
            creditCardId = cardId,
            installmentNumber = 1,
            totalInstallments = 2
        )
        val foreignSecond = TransactionEntity(
            title = "Parcela 2",
            amount = 5000,
            date = LocalDate.of(2026, 12, 5),
            responsibleId = otherUserId,
            accountId = accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            recurrenceGroupId = groupId,
            creditCardId = cardId,
            installmentNumber = 2,
            totalInstallments = 2
        )
        `when`(transactionRepository.findById(ownFirst.id)).thenReturn(ownFirst)
        `when`(transactionRepository.findFutureTransactionsInGroup(groupId, ownFirst.date, 1))
            .thenReturn(listOf(foreignSecond))

        assertThrows<UnauthorizedException> {
            deleteTransactionUseCase.execute(userId, ownFirst.id, deleteAllFuture = true)
        }
    }
}
