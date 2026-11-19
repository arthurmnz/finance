package com.finance.application.use_case.group

import com.finance.application.dto.group.SettleDebtRequest
import com.finance.application.service.AccountAccessService
import com.finance.domain.entity.AccountEntity
import com.finance.domain.entity.DebtSettlementEntity
import com.finance.domain.entity.GroupEntity
import com.finance.domain.entity.UserEntity
import com.finance.domain.enum.AccountType
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.InvalidTransactionException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.DebtSettlementRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
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
import org.mockito.Mockito.verify
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class SettleGroupDebtUseCaseTest {

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(): T {
        Mockito.any<T>()
        return null as T
    }

    private lateinit var groupRepository: GroupRepository
    private lateinit var groupMemberRepository: GroupMemberRepository
    private lateinit var jointAccountMemberRepository: JointAccountMemberRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var debtSettlementRepository: DebtSettlementRepository
    private lateinit var userRepository: UserRepository
    private lateinit var accountAccessService: AccountAccessService
    private lateinit var useCase: SettleGroupDebtUseCase

    @BeforeEach
    fun setup() {
        groupRepository = mock(GroupRepository::class.java)
        groupMemberRepository = mock(GroupMemberRepository::class.java)
        jointAccountMemberRepository = mock(JointAccountMemberRepository::class.java)
        accountRepository = mock(AccountRepository::class.java)
        transactionRepository = mock(TransactionRepository::class.java)
        debtSettlementRepository = mock(DebtSettlementRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        accountAccessService = AccountAccessService(groupMemberRepository, jointAccountMemberRepository)

        useCase = SettleGroupDebtUseCase(
            groupRepository = groupRepository,
            groupMemberRepository = groupMemberRepository,
            accountRepository = accountRepository,
            transactionRepository = transactionRepository,
            debtSettlementRepository = debtSettlementRepository,
            userRepository = userRepository,
            accountAccessService = accountAccessService
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
        assertFalse(response.isForgiveness)

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

    @Test
    fun `should fail if notes exceed 500 characters`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val userB = UserEntity(Name("Maria"), Name("Silva"), Email("maria@finance.com"), PasswordHash("hash"))
        val group = GroupEntity(id = UUID.randomUUID(), name = "Família")

        val request = SettleDebtRequest(
            creditorId = userB.id,
            amount = 20000,
            targetMonth = 11,
            targetYear = 2026,
            sourceAccountId = UUID.randomUUID(),
            destinationAccountId = UUID.randomUUID(),
            notes = "a".repeat(501)
        )

        assertThrows<InvalidTransactionException> {
            useCase.execute(group.id, userA.id, request)
        }
    }

    @Test
    fun `should fail if targetYear is outside 1970 to 2100`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val userB = UserEntity(Name("Maria"), Name("Silva"), Email("maria@finance.com"), PasswordHash("hash"))
        val group = GroupEntity(id = UUID.randomUUID(), name = "Família")

        assertThrows<InvalidTransactionException> {
            useCase.execute(
                group.id,
                userA.id,
                SettleDebtRequest(
                    creditorId = userB.id,
                    amount = 20000,
                    targetMonth = 11,
                    targetYear = 1969,
                    sourceAccountId = UUID.randomUUID(),
                    destinationAccountId = UUID.randomUUID()
                )
            )
        }

        assertThrows<InvalidTransactionException> {
            useCase.execute(
                group.id,
                userA.id,
                SettleDebtRequest(
                    creditorId = userB.id,
                    amount = 20000,
                    targetMonth = 11,
                    targetYear = 2101,
                    sourceAccountId = UUID.randomUUID(),
                    destinationAccountId = UUID.randomUUID()
                )
            )
        }

        assertThrows<InvalidTransactionException> {
            DebtSettlementEntity(
                groupId = group.id,
                debtorId = userA.id,
                creditorId = userB.id,
                amount = 20000,
                targetMonth = 11,
                targetYear = 1800
            )
        }
    }

    @Test
    fun `should reject joint account as source or destination in debt settlement`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val userB = UserEntity(Name("Maria"), Name("Silva"), Email("maria@finance.com"), PasswordHash("hash"))
        val group = GroupEntity(id = UUID.randomUUID(), name = "Família")

        val individualAcc = AccountEntity(
            ownerId = userB.id,
            name = "Conta Maria",
            startBalance = 50000,
            type = AccountType.CHECKING,
            tax = null
        )
        val jointAcc = AccountEntity.createJoint(
            groupId = group.id,
            name = "Conjunta",
            startBalance = 50000,
            type = AccountType.CHECKING
        )

        `when`(groupRepository.findById(group.id)).thenReturn(group)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, userA.id)).thenReturn(true)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, userB.id)).thenReturn(true)
        `when`(accountRepository.findById(jointAcc.id)).thenReturn(jointAcc)
        `when`(accountRepository.findById(individualAcc.id)).thenReturn(individualAcc)

        assertThrows<InvalidTransactionException> {
            useCase.execute(
                group.id,
                userA.id,
                SettleDebtRequest(
                    creditorId = userB.id,
                    amount = 10000,
                    targetMonth = 11,
                    targetYear = 2026,
                    sourceAccountId = jointAcc.id,
                    destinationAccountId = individualAcc.id
                )
            )
        }

        assertThrows<InvalidTransactionException> {
            useCase.execute(
                group.id,
                userA.id,
                SettleDebtRequest(
                    creditorId = userB.id,
                    amount = 10000,
                    targetMonth = 11,
                    targetYear = 2026,
                    sourceAccountId = individualAcc.id,
                    destinationAccountId = jointAcc.id
                )
            )
        }
    }

    @Test
    fun `should fail if source account belongs to another group member`() {
        val userA = UserEntity(Name("Arthur"), Name("Menezes"), Email("arthur@finance.com"), PasswordHash("hash"))
        val userB = UserEntity(Name("Maria"), Name("Silva"), Email("maria@finance.com"), PasswordHash("hash"))
        val group = GroupEntity(id = UUID.randomUUID(), name = "Família")

        val otherMemberSourceAcc = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userB.id,
            name = "Conta Maria",
            startBalance = 100000,
            balance = 100000,
            type = AccountType.CHECKING,
            tax = null,
            groupId = group.id
        )
        val destAccount = AccountEntity(
            id = UUID.randomUUID(),
            ownerId = userB.id,
            name = "Poupança Maria",
            startBalance = 50000,
            balance = 50000,
            type = AccountType.SAVINGS,
            tax = null,
            groupId = group.id
        )

        `when`(groupRepository.findById(group.id)).thenReturn(group)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, userA.id)).thenReturn(true)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(group.id, userB.id)).thenReturn(true)
        `when`(accountRepository.findById(otherMemberSourceAcc.id)).thenReturn(otherMemberSourceAcc)
        `when`(accountRepository.findById(destAccount.id)).thenReturn(destAccount)

        assertThrows<AccessDeniedException> {
            useCase.execute(
                group.id,
                userA.id,
                SettleDebtRequest(
                    creditorId = userB.id,
                    amount = 10000,
                    targetMonth = 11,
                    targetYear = 2026,
                    sourceAccountId = otherMemberSourceAcc.id,
                    destinationAccountId = destAccount.id
                )
            )
        }
    }
}
