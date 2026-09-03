package com.finance.domain.entity

import com.finance.domain.enum.GroupRole
import java.time.LocalDateTime
import java.util.UUID

class GroupMemberEntity(
    val id: UUID,
    val groupId: UUID,
    val userId: UUID,
    role: GroupRole,
    val joinedAt: LocalDateTime = LocalDateTime.now(),
) {
    var role: GroupRole = role
        private set

    constructor(groupId: UUID, userId: UUID, role: GroupRole) : this(
        id = UUID.randomUUID(),
        groupId = groupId,
        userId = userId,
        role = role,
        joinedAt = LocalDateTime.now(),
    )

    fun updateRole(newRole: GroupRole) {
        require(newRole != GroupRole.OWNER) {
            "Para transferir a titularidade, use o endpoint de transferência de ownership."
        }
        this.role = newRole
    }

    fun promoteToOwner() {
        this.role = GroupRole.OWNER
    }

    fun demoteToAdmin() {
        this.role = GroupRole.ADMIN
    }

    fun isOwner(): Boolean = role == GroupRole.OWNER
    fun isAdmin(): Boolean = role == GroupRole.ADMIN
    fun isMember(): Boolean = role == GroupRole.MEMBER
    fun canManageMembers(): Boolean = role == GroupRole.OWNER || role == GroupRole.ADMIN
    fun canManageGroup(): Boolean = role == GroupRole.OWNER
    fun canRenameGroup(): Boolean = role == GroupRole.OWNER || role == GroupRole.ADMIN

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupMemberEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
