package com.finance.application.mapper

import com.finance.application.dto.account.AccountGroupRequestResponse
import com.finance.application.dto.account.AccountResponse
import com.finance.application.dto.group.GroupUserSummary
import com.finance.domain.entity.AccountEntity
import com.finance.domain.entity.AccountGroupRequestEntity
import com.finance.domain.entity.UserEntity
import org.springframework.stereotype.Component

@Component
class AccountDtoMapper {

    fun toAccountResponse(account: AccountEntity): AccountResponse {
        return AccountResponse(
            id = account.id,
            ownerId = account.ownerId,
            name = account.name,
            startBalance = account.startBalance,
            balance = account.balance,
            type = account.type,
            tax = account.tax,
            groupId = account.groupId,
            createdAt = account.createdAt,
            updatedAt = account.updatedAt
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
