package com.finance.infraestructure.persistence.repository

import com.finance.domain.entity.GroupInvitationEntity
import com.finance.domain.enum.GroupInvitationStatus
import com.finance.domain.repository.GroupInvitationRepository
import com.finance.infraestructure.persistence.mapper.GroupInvitationMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
class GroupInvitationRepositoryImpl(
    private val springDataGroupInvitationRepository: SpringDataGroupInvitationRepository,
    private val groupInvitationMapper: GroupInvitationMapper,
) : GroupInvitationRepository {

    @Transactional
    override fun save(invitation: GroupInvitationEntity): GroupInvitationEntity {
        val jpaEntity = groupInvitationMapper.toJpaEntity(invitation)
        val saved = springDataGroupInvitationRepository.save(jpaEntity)
        return groupInvitationMapper.toDomain(saved)
    }

    @Transactional(readOnly = true)
    override fun findById(id: UUID): GroupInvitationEntity? {
        return springDataGroupInvitationRepository.findById(id)
            .map { groupInvitationMapper.toDomain(it) }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByGroupIdAndInviteeIdAndStatus(
        groupId: UUID,
        inviteeId: UUID,
        status: GroupInvitationStatus,
    ): GroupInvitationEntity? {
        return springDataGroupInvitationRepository
            .findByGroupIdAndInviteeIdAndStatus(groupId, inviteeId, status)
            ?.let { groupInvitationMapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findAllByInviteeIdAndStatus(
        inviteeId: UUID,
        status: GroupInvitationStatus,
    ): List<GroupInvitationEntity> {
        return springDataGroupInvitationRepository
            .findAllByInviteeIdAndStatus(inviteeId, status)
            .map { groupInvitationMapper.toDomain(it) }
    }

    @Transactional
    override fun deleteAllByGroupId(groupId: UUID) {
        springDataGroupInvitationRepository.deleteAllByGroupId(groupId)
    }
}
