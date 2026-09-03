package com.finance.infraestructure.persistence.repository

import com.finance.domain.enum.GroupInvitationStatus
import com.finance.infraestructure.persistence.entity.GroupInvitationJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SpringDataGroupInvitationRepository : JpaRepository<GroupInvitationJpaEntity, UUID> {

    fun findByGroupIdAndInviteeIdAndStatus(
        groupId: UUID,
        inviteeId: UUID,
        status: GroupInvitationStatus,
    ): GroupInvitationJpaEntity?

    fun findAllByInviteeIdAndStatus(
        inviteeId: UUID,
        status: GroupInvitationStatus,
    ): List<GroupInvitationJpaEntity>

    fun deleteAllByGroupId(groupId: UUID)
}
