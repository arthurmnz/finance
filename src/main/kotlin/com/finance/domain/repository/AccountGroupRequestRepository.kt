package com.finance.domain.repository

import com.finance.domain.entity.AccountGroupRequestEntity
import com.finance.domain.enum.AccountGroupRequestStatus
import java.util.UUID

interface AccountGroupRequestRepository {
    fun save(request: AccountGroupRequestEntity): AccountGroupRequestEntity
    fun findById(id: UUID): AccountGroupRequestEntity?
    fun findByAccountIdAndStatus(accountId: UUID, status: AccountGroupRequestStatus): AccountGroupRequestEntity?
    fun findAllByGroupIdAndStatus(groupId: UUID, status: AccountGroupRequestStatus): List<AccountGroupRequestEntity>
    fun deleteAllByGroupId(groupId: UUID)
    fun deleteAllByAccountId(accountId: UUID)
}
