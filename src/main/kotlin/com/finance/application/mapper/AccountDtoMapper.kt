package com.finance.application.mapper

import com.finance.application.dto.account.AccountGroupRequestResponse
import com.finance.application.dto.account.AccountResponse
import com.finance.application.dto.account.GroupAccountResponse
import com.finance.application.dto.group.GroupUserSummary
import com.finance.domain.entity.AccountEntity
import com.finance.domain.entity.AccountGroupRequestEntity
import com.finance.domain.entity.UserEntity
import org.springframework.stereotype.Component

@Component
class AccountDtoMapper {

    fun toUserSummary(user: UserEntity): GroupUserSummary {
        return GroupUserSummary(
            id = user.id,
            firstName = user.firstName.value,
            lastName = user.lastName.value,
            email = user.email.normalized
        )
    }

    fun toSortedUserSummaries(users: Collection<UserEntity>): List<GroupUserSummary> {
        return users.map { toUserSummary(it) }
            .sortedWith(compareBy({ it.firstName.lowercase() }, { it.lastName.lowercase() }, { it.id }))
    }

    fun toAccountResponse(
        account: AccountEntity,
        allowedMembers: List<GroupUserSummary> = emptyList()
    ): AccountResponse {
        return AccountResponse(
            id = account.id,
            ownerId = account.ownerId,
            name = account.name,
            startBalance = account.startBalance,
            balance = account.balance,
            type = account.type,
            tax = account.tax,
            groupId = account.groupId,
            includeInTotal = account.includeInTotal,
            createdAt = account.createdAt,
            updatedAt = account.updatedAt,
            isJoint = account.isJoint,
            allowedMembers = allowedMembers
        )
    }

    fun toGroupAccountResponse(
        account: AccountEntity,
        owner: GroupUserSummary?,
        allowedMembers: List<GroupUserSummary> = emptyList()
    ): GroupAccountResponse {
        return GroupAccountResponse(
            id = account.id,
            name = account.name,
            startBalance = account.startBalance,
            balance = account.balance,
            type = account.type,
            tax = account.tax,
            groupId = account.groupId,
            includeInTotal = account.includeInTotal,
            createdAt = account.createdAt,
            updatedAt = account.updatedAt,
            owner = owner,
            isJoint = account.isJoint,
            allowedMembers = allowedMembers
        )
    }

    fun toAccountGroupRequestResponse(
        request: AccountGroupRequestEntity,
        account: AccountEntity,
        requester: UserEntity
    ): AccountGroupRequestResponse {
        return AccountGroupRequestResponse(
            id = request.id,
            account = AccountGroupRequestResponse.AccountSummary(
                id = account.id,
                name = account.name
            ),
            requester = GroupUserSummary(
                id = requester.id,
                firstName = requester.firstName.value,
                lastName = requester.lastName.value,
                email = requester.email.normalized
            ),
            status = request.status,
            createdAt = request.createdAt,
            updatedAt = request.updatedAt
        )
    }
}
