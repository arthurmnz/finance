package com.finance.domain.repository

import com.finance.domain.entity.GroupMemberEntity
import com.finance.domain.enum.GroupRole
import java.util.UUID

interface GroupMemberRepository {
    fun save(member: GroupMemberEntity): GroupMemberEntity
    fun findByGroupIdAndUserId(groupId: UUID, userId: UUID): GroupMemberEntity?
    fun findAllByGroupId(groupId: UUID): List<GroupMemberEntity>
    fun findAllByUserId(userId: UUID): List<GroupMemberEntity>
    fun findOldestAdminByGroupId(groupId: UUID): GroupMemberEntity?
    fun findOldestMemberByGroupIdExcludingOwner(groupId: UUID): GroupMemberEntity?
    fun countByGroupId(groupId: UUID): Int
    fun existsByGroupIdAndUserId(groupId: UUID, userId: UUID): Boolean
    fun deleteByGroupIdAndUserId(groupId: UUID, userId: UUID)
    fun deleteAllByGroupId(groupId: UUID)
    fun deleteAllByUserId(userId: UUID)
}
