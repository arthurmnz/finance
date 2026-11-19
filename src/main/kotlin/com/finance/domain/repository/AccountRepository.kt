package com.finance.domain.repository

import com.finance.domain.entity.AccountEntity
import java.util.UUID

interface AccountRepository {
    fun save(account: AccountEntity): AccountEntity
    fun findById(id: UUID): AccountEntity?
    fun findAllByIdIn(ids: Collection<UUID>): List<AccountEntity>
    fun findAllByOwnerId(ownerId: UUID): List<AccountEntity>
    fun findAllByGroupId(groupId: UUID): List<AccountEntity>
    fun findAllByGroupIdIn(groupIds: Collection<UUID>): List<AccountEntity>
    fun findAllByGroupIdAndIsJointTrue(groupId: UUID): List<AccountEntity>
    fun existsByOwnerIdAndGroupIdNotNull(ownerId: UUID): Boolean
    fun existsByOwnerIdAndGroupId(ownerId: UUID, groupId: UUID): Boolean
    fun deleteAllByOwnerId(ownerId: UUID)
    fun deleteAllByGroupIdAndIsJointTrue(groupId: UUID)
    fun deleteById(id: UUID)
}
