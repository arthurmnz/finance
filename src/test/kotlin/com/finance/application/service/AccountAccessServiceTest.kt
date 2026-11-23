package com.finance.application.service

import com.finance.domain.entity.CreditCardEntity
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.CreditCardNotFoundException
import com.finance.domain.exception.UnauthorizedException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.CreditCardRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.JointAccountMemberRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.time.LocalDate
import java.util.UUID

class AccountAccessServiceTest {

    private lateinit var groupMemberRepository: GroupMemberRepository
    private lateinit var jointAccountMemberRepository: JointAccountMemberRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var creditCardRepository: CreditCardRepository
    private lateinit var accountAccessService: AccountAccessService

    @BeforeEach
    fun setup() {
        groupMemberRepository = mock(GroupMemberRepository::class.java)
        jointAccountMemberRepository = mock(JointAccountMemberRepository::class.java)
        accountRepository = mock(AccountRepository::class.java)
        creditCardRepository = mock(CreditCardRepository::class.java)
        accountAccessService = AccountAccessService(groupMemberRepository, jointAccountMemberRepository)
    }

    @Test
    fun `should allow card owner and active group member, and reject non-author, missing card, or former group member`() {
        val ownerId = UUID.randomUUID()
        val memberId = UUID.randomUUID()
        val groupId = UUID.randomUUID()
        val accountId = UUID.randomUUID()

        val card = CreditCardEntity(
            id = UUID.randomUUID(),
            ownerId = ownerId,
            accountId = accountId,
            name = "Cartão Família",
            limitAmount = 500000,
            closingDay = 10,
            dueDay = 17,
            groupId = groupId
        )

        val ownerTx = TransactionEntity(
            title = "Compra Owner",
            amount = 5000,
            date = LocalDate.of(2026, 11, 10),
            responsibleId = ownerId,
            accountId = accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            creditCardId = card.id
        )
        val memberTx = TransactionEntity(
            title = "Compra Membro",
            amount = 5000,
            date = LocalDate.of(2026, 11, 10),
            responsibleId = memberId,
            accountId = accountId,
            type = TransactionType.EXPENSE,
            destinationAccountId = null,
            categoryId = null,
            status = TransactionStatus.COMPLETED,
            creditCardId = card.id
        )

        `when`(creditCardRepository.findById(card.id)).thenReturn(card)
        `when`(groupMemberRepository.existsByGroupIdAndUserId(groupId, memberId)).thenReturn(true)

        accountAccessService.requireTransactionMutationAccess(
            transaction = ownerTx,
            userId = ownerId,
            accountRepository = accountRepository,
            creditCardRepository = creditCardRepository,
            unauthorizedMessage = "Unauthorized"
        )
        accountAccessService.requireTransactionMutationAccess(
            transaction = memberTx,
            userId = memberId,
            accountRepository = accountRepository,
            creditCardRepository = creditCardRepository,
            unauthorizedMessage = "Unauthorized"
        )

        // Outro usuário não autor da transação -> UnauthorizedException
        assertThrows<UnauthorizedException> {
            accountAccessService.requireTransactionMutationAccess(
                transaction = memberTx,
                userId = ownerId,
                accountRepository = accountRepository,
                creditCardRepository = creditCardRepository,
                unauthorizedMessage = "Unauthorized"
            )
        }

        // Ex-membro removido do grupo -> AccessDeniedException
        `when`(groupMemberRepository.existsByGroupIdAndUserId(groupId, memberId)).thenReturn(false)
        assertThrows<AccessDeniedException> {
            accountAccessService.requireTransactionMutationAccess(
                transaction = memberTx,
                userId = memberId,
                accountRepository = accountRepository,
                creditCardRepository = creditCardRepository,
                unauthorizedMessage = "Unauthorized"
            )
        }

        // Cartão inexistente -> CreditCardNotFoundException
        `when`(creditCardRepository.findById(card.id)).thenReturn(null)
        assertThrows<CreditCardNotFoundException> {
            accountAccessService.requireTransactionMutationAccess(
                transaction = ownerTx,
                userId = ownerId,
                accountRepository = accountRepository,
                creditCardRepository = creditCardRepository,
                unauthorizedMessage = "Unauthorized"
            )
        }
    }
}
