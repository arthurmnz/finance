package com.finance.application.use_case.account

import com.finance.application.dto.account.GroupAccountResponse
import com.finance.application.dto.group.GroupUserSummary
import com.finance.application.service.GroupService
import com.finance.application.service.UserService
import com.finance.domain.repository.AccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ListGroupAccountsUseCase(
    private val groupService: GroupService,
    private val userService: UserService,
    private val accountRepository: AccountRepository,
) {

    @Transactional(readOnly = true)
    fun execute(groupId: UUID, authenticatedUserId: UUID): List<GroupAccountResponse> {
        // Valida se o usuário é membro do grupo
        groupService.findMember(groupId, authenticatedUserId)

        return accountRepository.findAllByGroupId(groupId).map { account ->
            val user = userService.findById(account.ownerId)
            
            GroupAccountResponse(
                id = account.id,
                name = account.name,
                startBalance = account.startBalance,
                balance = account.balance,
                type = account.type,
                tax = account.tax,
                groupId = account.groupId,
                createdAt = account.createdAt,
                updatedAt = account.updatedAt,
                owner = GroupUserSummary(
                    id = user.id,
                    firstName = user.firstName.value,
                    lastName = user.lastName.value,
                    email = user.email.normalized
                )
            )
        }
    }
}
