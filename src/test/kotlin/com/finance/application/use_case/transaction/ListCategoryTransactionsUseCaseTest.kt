package com.finance.application.use_case.transaction

import com.finance.application.service.AccountAccessService
import com.finance.domain.entity.AccountEntity
import com.finance.domain.entity.CategoryEntity
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.entity.UserEntity
import com.finance.domain.enum.AccountType
import com.finance.domain.enum.CategoryLevel
import com.finance.domain.enum.CategoryType
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.CreditCardRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.JointAccountMemberRepository
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.repository.UserRepository
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

class ListCategoryTransactionsUseCaseTest {

    @Test
    fun `personal category should hide transactions of joint account when access was revoked`() {
        val transactionRepository = mock(TransactionRepository::class.java)
        val categoryRepository = mock(CategoryRepository::class.java)
        val groupMemberRepository = mock(GroupMemberRepository::class.java)
        val jointRepo = mock(JointAccountMemberRepository::class.java)
        val userRepository = mock(UserRepository::class.java)
        val accountRepository = mock(AccountRepository::class.java)
        val creditCardRepository = mock(CreditCardRepository::class.java)
        val useCase = ListCategoryTransactionsUseCase(
            transactionRepository, categoryRepository, groupMemberRepository, userRepository,
            accountRepository, creditCardRepository, AccountAccessService(groupMemberRepository, jointRepo)
        )

        val user = UserEntity(
            id = UUID.randomUUID(), firstName = Name("Ana"), lastName = Name("Silva"),
            email = Email("ana@finance.com"), passwordHash = PasswordHash("h")
        )
        val category = CategoryEntity(
            id = UUID.randomUUID(), name = "Casa", description = null, userId = user.id, groupId = null,
            type = CategoryType.EXPENSE, color = "#000000", icon = "x", level = CategoryLevel.GENERAL
        )
        val groupId = UUID.randomUUID()
        val joint = AccountEntity.createJoint(groupId, "Conjunta", 1000, AccountType.CHECKING)
        val personal = AccountEntity(ownerId = user.id, name = "Pessoal", startBalance = 0, type = AccountType.CHECKING, tax = null)
        fun tx(title: String, acc: UUID) = TransactionEntity(
            title = title, amount = 100, date = LocalDate.of(2026, 1, 1), responsibleId = user.id,
            accountId = acc, type = TransactionType.EXPENSE, destinationAccountId = null,
            categoryId = category.id, status = TransactionStatus.COMPLETED
        )
        val txs = listOf(tx("Conjunta", joint.id), tx("Pessoal", personal.id))

        `when`(categoryRepository.findById(category.id)).thenReturn(category)
        `when`(categoryRepository.findByParentId(category.id)).thenReturn(emptyList())
        `when`(transactionRepository.findByCategoryIdInOrGeneralCategoryId(listOf(category.id), category.id)).thenReturn(txs)
        `when`(accountRepository.findAllByIdIn(setOf(joint.id, personal.id))).thenReturn(listOf(joint, personal))
        `when`(userRepository.findAll()).thenReturn(listOf(user))

        val result = useCase.execute(category.id, null, null, user.id)
        assertEquals(listOf("Pessoal"), result.map { it.title })
    }
}
