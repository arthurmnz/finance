package com.finance.application.use_case.account

import com.finance.application.dto.account.UpdateAccountRequest
import com.finance.application.mapper.AccountDtoMapper
import com.finance.application.service.AccountAccessService
import com.finance.domain.entity.AccountEntity
import com.finance.domain.entity.GroupMemberEntity
import com.finance.domain.enum.AccountType
import com.finance.domain.enum.GroupRole
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.JointAccountMemberRepository
import com.finance.domain.repository.UserRepository
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID
import kotlin.test.assertEquals

class UpdateAccountUseCaseTest {

    @Test
    fun `editing startBalance of joint account preserves transactional balance via updateJoint`() {
        val accountRepository = mock(AccountRepository::class.java)
        val groupMemberRepository = mock(GroupMemberRepository::class.java)
        val jointRepo = mock(JointAccountMemberRepository::class.java)
        val userRepository = mock(UserRepository::class.java)
        val useCase = UpdateAccountUseCase(
            accountRepository, AccountDtoMapper(), AccountAccessService(groupMemberRepository, jointRepo),
            jointRepo, userRepository
        )

        val groupId = UUID.randomUUID()
        val adminId = UUID.randomUUID()
        val joint = AccountEntity(
            id = UUID.randomUUID(), ownerId = null, name = "Conjunta", startBalance = 5000, balance = 7000,
            type = AccountType.CHECKING, tax = null, groupId = groupId, isJoint = true
        )
        `when`(accountRepository.findById(joint.id)).thenReturn(joint)
        `when`(accountRepository.save(any(AccountEntity::class.java) ?: joint)).thenAnswer { it.getArgument<AccountEntity>(0) }
        `when`(groupMemberRepository.findByGroupIdAndUserId(groupId, adminId))
            .thenReturn(GroupMemberEntity(groupId = groupId, userId = adminId, role = GroupRole.ADMIN))
        `when`(jointRepo.findAllByAccountId(joint.id)).thenReturn(emptyList())
        `when`(userRepository.findAllByIdIn(emptyList())).thenReturn(emptyList())

        useCase.execute(joint.id, UpdateAccountRequest("Conjunta", 6000, AccountType.CHECKING), adminId)

        assertEquals(6000, joint.startBalance)
        assertEquals(8000, joint.balance)
    }
}
