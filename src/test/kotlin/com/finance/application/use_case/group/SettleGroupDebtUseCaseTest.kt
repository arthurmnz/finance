package com.finance.application.use_case.group

import com.finance.application.dto.group.SettleDebtRequest
import com.finance.domain.entity.AccountEntity
import com.finance.domain.entity.GroupEntity
import com.finance.domain.entity.UserEntity
import com.finance.domain.enum.AccountType
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.DebtSettlementRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
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
import org.mockito.Mockito.verify
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SettleGroupDebtUseCaseTest {

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(): T {
        Mockito.any<T>()
        return null as T
    }

    private lateinit var groupRepository: GroupRepository
    private lateinit var groupMemberRepository: GroupMemberRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var debtSettlementRepository: DebtSettlementRepository
    private lateinit var userRepository: UserRepository
    private lateinit var useCase: SettleGroupDebtUseCase

    @BeforeEach
    fun setup() {
        groupRepository = mock(GroupRepository::class.java)
        groupMemberRepository = mock(GroupMemberRepository::class.java)
        accountRepository = mock(AccountRepository::class.java)
        transactionRepository = mock(TransactionRepository::class.java)
        debtSettlementRepository = mock(DebtSettlementRepository::class.java)
        userRepository = mock(UserRepository::class.java)

        useCase = SettleGroupDebtUseCase(
            groupRepository = groupRepository,
            groupMemberRepository = groupMemberRepository,
            accountRepository = accountRepository,
            transactionRepository = transactionRepository,
            debtSettlementRepository = debtSettlementRepository,
            userRepository = userRepository
        )
    }

    @Test
    fun `should settle debt successfully with transfer transaction and balance update`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val userB = UserEntity(Name("Maria"), Name("Silva"), Email("maria@finance.com"), PasswordHash("hash"))
        val group = GroupEntity(id = UUID.randomUUID(), name = "Família")

        val sourceAccount = AccountEntity(
            ownerId = userA.id,
            name = "Conta Arthur",
            startBalance = 100000,
            type = AccountType.CHECKING,
            tax = null
        )
        val destAccount = AccountEntity(
            ownerId = userB.id,
            name = "Conta Maria",
            startBalance = 50000,
            type = AccountType.CHECKING,
            tax = null
        )

        `when`(groupRepository.findById(group.id)).thenReturn(group)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, userA.id)).thenReturn(true)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, userB.id)).thenReturn(true)
        `when`(accountRepository.findById(sourceAccount.id)).thenReturn(sourceAccount)
        `when`(accountRepository.findById(destAccount.id)).thenReturn(destAccount)
        `when`(userRepository.findById(userA.id)).thenReturn(userA)
        `when`(userRepository.findById(userB.id)).thenReturn(userB)

        `when`(transactionRepository.save(anyNonNull())).thenAnswer { it.arguments[0] }
        `when`(debtSettlementRepository.save(anyNonNull())).thenAnswer { it.arguments[0] }

        val request = SettleDebtRequest(
            creditorId = userB.id,
            amount = 20000,
            targetMonth = 11,
            targetYear = 2026,
            sourceAccountId = sourceAccount.id,
            destinationAccountId = destAccount.id,
            notes = "Pagamento parcial fatura nov"
        )

        val response = useCase.execute(group.id, userA.id, request)

        assertNotNull(response)
        assertEquals(20000, response.amount)
        assertEquals(userA.id, response.debtorId)
        assertEquals(userB.id, response.creditorId)
        assertEquals(11, response.targetMonth)
        assertEquals(2026, response.targetYear)

        // Saldos devem ter sido atualizados
        assertEquals(80000, sourceAccount.balance)
        assertEquals(70000, destAccount.balance)

        verify(accountRepository).save(sourceAccount)
        verify(accountRepository).save(destAccount)
        verify(transactionRepository).save(anyNonNull())
        verify(debtSettlementRepository).save(anyNonNull())
    }

    @Test
    fun `should fail if destination account does not belong to creditor`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val userB = UserEntity(Name("Maria"), Name("Silva"), Email("maria@finance.com"), PasswordHash("hash"))
        val group = GroupEntity(id = UUID.randomUUID(), name = "Família")

        val sourceAccount = AccountEntity(
            ownerId = userA.id,
            name = "Conta Arthur",
            startBalance = 100000,
            type = AccountType.CHECKING,
            tax = null
        )
        // Conta pertence a terceiro
        val destAccount = AccountEntity(
            ownerId = UUID.randomUUID(),
            name = "Conta Outro",
            startBalance = 50000,
            type = AccountType.CHECKING,
            tax = null
        )

        `when`(groupRepository.findById(group.id)).thenReturn(group)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, userA.id)).thenReturn(true)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, userB.id)).thenReturn(true)
        `when`(accountRepository.findById(sourceAccount.id)).thenReturn(sourceAccount)
        `when`(accountRepository.findById(destAccount.id)).thenReturn(destAccount)

        val request = SettleDebtRequest(
            creditorId = userB.id,
            amount = 20000,
            targetMonth = 11,
            targetYear = 2026,
            sourceAccountId = sourceAccount.id,
            destinationAccountId = destAccount.id
        )

        assertThrows<AccessDeniedException> {
            useCase.execute(group.id, userA.id, request)
        }
    }
}
