package com.finance.application.use_case.account

import com.finance.domain.entity.AccountEntity
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.enum.AccountType
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.TransactionRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

class GetTotalBalanceUseCaseTest {

    private lateinit var accountRepository: AccountRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var useCase: GetTotalBalanceUseCase

    @BeforeEach
    fun setup() {
        accountRepository = mock(AccountRepository::class.java)
        transactionRepository = mock(TransactionRepository::class.java)
        useCase = GetTotalBalanceUseCase(accountRepository, transactionRepository)
    }

    @Test
    fun `should isolate expenses between User A and User B on shared credit card`() {
        val userAId = UUID.randomUUID()
        val userBId = UUID.randomUUID()

        val accountA = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userAId,
            name = "Conta Corrente A",
            startBalance = 100000,
            balance = 100000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )
        val cardAId = UUID.randomUUID()

        // Para User A:
        `when`(accountRepository.findAllByOwnerId(userAId)).thenReturn(listOf(accountA))
        // Para User B (não possui conta bancária própria direta):
        `when`(accountRepository.findAllByOwnerId(userBId)).thenReturn(emptyList())

        val startDate = LocalDate.of(2026, 10, 1)
        val endDate = LocalDate.of(2026, 10, 31)

        // Compra própria de User A no cartão (R$ 50)
        val txA = TransactionEntity(
            title = "Compra User A",
            amount = 5000,
            date = LocalDate.of(2026, 10, 15),
            responsibleId = userAId,
            accountId = accountA.id,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            creditCardId = cardAId,
            invoiceMonth = 11,
            invoiceYear = 2026
        )

        // Compras de User B no cartão compartilhado de User A (R$ 30 e R$ 20)
        val txB1 = TransactionEntity(
            title = "Uber User B",
            amount = 3000,
            date = LocalDate.of(2026, 10, 18),
            responsibleId = userBId,
            accountId = accountA.id,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            creditCardId = cardAId,
            invoiceMonth = 11,
            invoiceYear = 2026
        )
        val txB2 = TransactionEntity(
            title = "iFood User B",
            amount = 2000,
            date = LocalDate.of(2026, 10, 20),
            responsibleId = userBId,
            accountId = accountA.id,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            creditCardId = cardAId,
            invoiceMonth = 11,
            invoiceYear = 2026
        )

        // Repositório para transações na conta A (todas estão vinculadas a accountA)
        `when`(transactionRepository.findByAccountIdInAndDateBetween(listOf(accountA.id), startDate, endDate))
            .thenReturn(listOf(txA, txB1, txB2))

        // Repositório para transações sob responsabilidade de User A
        `when`(transactionRepository.findByResponsibleIdAndDateBetween(userAId, startDate, endDate))
            .thenReturn(listOf(txA))

        // Repositório para transações sob responsabilidade de User B
        `when`(transactionRepository.findByResponsibleIdAndDateBetween(userBId, startDate, endDate))
            .thenReturn(listOf(txB1, txB2))

        // Consulta de User A para outubro/2026
        val balanceA = useCase.execute(userAId, null, month = 10, year = 2026)
        // User A só deve ter os seus próprios 5000 de despesa, ignorando as compras de User B!
        assertEquals(5000, balanceA.monthlyExpense)
        assertEquals(0, balanceA.monthlyIncome)

        // Consulta de User B para outubro/2026
        val balanceB = useCase.execute(userBId, null, month = 10, year = 2026)
        // User B deve ver as suas despesas feitas no cartão compartilhado (3000 + 2000 = 5000)
        assertEquals(5000, balanceB.monthlyExpense)
        assertEquals(0, balanceB.monthlyIncome)
    }

    @Test
    fun `should respect ignoreAccountIds for both bank transactions and card transactions`() {
        val userId = UUID.randomUUID()
        val account1 = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userId,
            name = "Conta 1",
            startBalance = 100000,
            balance = 100000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )
        val account2 = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userId,
            name = "Conta Ignorada",
            startBalance = 50000,
            balance = 50000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = null
        )

        `when`(accountRepository.findAllByOwnerId(userId)).thenReturn(listOf(account1, account2))

        val startDate = LocalDate.of(2026, 10, 1)
        val endDate = LocalDate.of(2026, 10, 31)

        val tx1 = TransactionEntity(
            title = "Gasto Conta 1",
            amount = 4000,
            date = LocalDate.of(2026, 10, 5),
            responsibleId = userId,
            accountId = account1.id,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED
        )

        val txCardOnIgnoredAccount = TransactionEntity(
            title = "Gasto Cartão Conta Ignorada",
            amount = 9000,
            date = LocalDate.of(2026, 10, 6),
            responsibleId = userId,
            accountId = account2.id,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            creditCardId = UUID.randomUUID()
        )

        `when`(transactionRepository.findByAccountIdInAndDateBetween(listOf(account1.id), startDate, endDate))
            .thenReturn(listOf(tx1))

        `when`(transactionRepository.findByResponsibleIdAndDateBetween(userId, startDate, endDate))
            .thenReturn(listOf(tx1, txCardOnIgnoredAccount))

        val result = useCase.execute(userId, setOf(account2.id), month = 10, year = 2026)

        // Saldo total só de account1 (100000)
        assertEquals(100000, result.total)
        // Despesa apenas de account1 (4000), o cartão da conta ignorada foi descartado
        assertEquals(4000, result.monthlyExpense)
    }
}
