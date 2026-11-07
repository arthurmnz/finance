package com.finance.application.use_case.user

import com.finance.application.service.InMemoryUserRepository
import com.finance.application.service.UserService
import com.finance.domain.entity.UserEntity
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.UserNotFoundException
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import com.finance.domain.repository.AccountGroupRequestRepository
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.GroupInvitationRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertNull

class DeleteUserUseCaseTest {

    private lateinit var userRepository: InMemoryUserRepository
    private lateinit var userService: UserService
    private lateinit var groupMemberRepository: GroupMemberRepository
    private lateinit var groupRepository: GroupRepository
    private lateinit var groupInvitationRepository: GroupInvitationRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var accountGroupRequestRepository: AccountGroupRequestRepository
    private lateinit var deleteUserUseCase: DeleteUserUseCase

    @BeforeEach
    fun setup() {
        userRepository = InMemoryUserRepository()
        userService = UserService(userRepository)
        groupMemberRepository = mock(GroupMemberRepository::class.java)
        groupRepository = mock(GroupRepository::class.java)
        groupInvitationRepository = mock(GroupInvitationRepository::class.java)
        accountRepository = mock(AccountRepository::class.java)
        accountGroupRequestRepository = mock(AccountGroupRequestRepository::class.java)

        deleteUserUseCase = DeleteUserUseCase(
            userRepository = userRepository,
            userService = userService,
            groupMemberRepository = groupMemberRepository,
            groupRepository = groupRepository,
            groupInvitationRepository = groupInvitationRepository,
            accountRepository = accountRepository,
            accountGroupRequestRepository = accountGroupRequestRepository,
        )
    }

    @Test
    fun `should delete user successfully`() {
        val user = UserEntity(
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur@finance.com"),
            passwordHash = PasswordHash("hash123")
        )
        userRepository.save(user)

        deleteUserUseCase.execute(user.id, authenticatedUserId = user.id)

        assertNull(userRepository.findById(user.id))
        assertFalse(userRepository.existsByEmail(Email("arthur@finance.com")))
    }

    @Test
    fun `should fail deletion if user does not exist`() {
        assertThrows<UserNotFoundException> {
            deleteUserUseCase.execute(UUID.randomUUID())
        }
    }

    @Test
    fun `should fail deletion if authenticated user is different`() {
        val user = UserEntity(
            firstName = Name("Arthur"),
            lastName = Name("Menezes"),
            email = Email("arthur@finance.com"),
            passwordHash = PasswordHash("hash123")
        )
        userRepository.save(user)

        assertThrows<AccessDeniedException> {
            deleteUserUseCase.execute(user.id, authenticatedUserId = UUID.randomUUID())
        }
    }
}
