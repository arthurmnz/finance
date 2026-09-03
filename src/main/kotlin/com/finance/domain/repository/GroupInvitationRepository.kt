package com.finance.domain.repository

import com.finance.domain.entity.GroupInvitationEntity
import com.finance.domain.enum.GroupInvitationStatus
import java.util.UUID

interface GroupInvitationRepository {
    fun save(invitation: GroupInvitationEntity): GroupInvitationEntity
    fun findById(id: UUID): GroupInvitationEntity?
    fun findByGroupIdAndInviteeIdAndStatus(
        groupId: UUID,
        inviteeId: UUID,
        status: GroupInvitationStatus,
    ): GroupInvitationEntity?
    fun findAllByInviteeIdAndStatus(inviteeId: UUID, status: GroupInvitationStatus): List<GroupInvitationEntity>
    fun deleteAllByGroupId(groupId: UUID)
}
