package com.finance.domain.repository

import com.finance.domain.entity.JointAccountMemberEntity
import java.util.UUID

interface JointAccountMemberRepository {
    fun save(member: JointAccountMemberEntity): JointAccountMemberEntity
    fun saveAll(members: List<JointAccountMemberEntity>): List<JointAccountMemberEntity>
    fun findAllByAccountId(accountId: UUID): List<JointAccountMemberEntity>
    fun findAllByAccountIdIn(accountIds: Collection<UUID>): List<JointAccountMemberEntity>
    fun findAllByUserId(userId: UUID): List<JointAccountMemberEntity>
    fun existsByAccountIdAndUserId(accountId: UUID, userId: UUID): Boolean
    fun deleteByAccountIdAndUserId(accountId: UUID, userId: UUID)
    fun deleteAllByAccountId(accountId: UUID)
    fun deleteAllByAccountIdInAndUserId(accountIds: Collection<UUID>, userId: UUID)
    fun deleteAllByUserId(userId: UUID)
}
