package com.finance.infraestructure.persistence.repository

import com.finance.domain.entity.GroupMemberEntity
import com.finance.domain.enum.GroupRole
import com.finance.domain.repository.GroupMemberRepository
import com.finance.infraestructure.persistence.mapper.GroupMemberMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
class GroupMemberRepositoryImpl(
    private val springDataGroupMemberRepository: SpringDataGroupMemberRepository,
    private val groupMemberMapper: GroupMemberMapper,
) : GroupMemberRepository {

    @Transactional
    override fun save(member: GroupMemberEntity): GroupMemberEntity {
        val jpaEntity = groupMemberMapper.toJpaEntity(member)
        val saved = springDataGroupMemberRepository.save(jpaEntity)
        return groupMemberMapper.toDomain(saved)
    }

    @Transactional(readOnly = true)
    override fun findByGroupIdAndUserId(groupId: UUID, userId: UUID): GroupMemberEntity? {
        return springDataGroupMemberRepository
            .findByGroupIdAndUserId(groupId, userId)
            ?.let { groupMemberMapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findAllByGroupId(groupId: UUID): List<GroupMemberEntity> {
        return springDataGroupMemberRepository
            .findAllByGroupId(groupId)
            .map { groupMemberMapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findAllByUserId(userId: UUID): List<GroupMemberEntity> {
        return springDataGroupMemberRepository
            .findAllByUserId(userId)
            .map { groupMemberMapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findOldestAdminByGroupId(groupId: UUID): GroupMemberEntity? {
        return springDataGroupMemberRepository
            .findOldestByGroupIdAndRole(groupId, GroupRole.ADMIN)
            ?.let { groupMemberMapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findOldestMemberByGroupIdExcludingOwner(groupId: UUID): GroupMemberEntity? {
        return springDataGroupMemberRepository
            .findOldestNonOwnerByGroupId(groupId)
            ?.let { groupMemberMapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun countByGroupId(groupId: UUID): Int {
        return springDataGroupMemberRepository.countByGroupId(groupId)
    }

    @Transactional(readOnly = true)
    override fun existsByGroupIdAndUserId(groupId: UUID, userId: UUID): Boolean {
        return springDataGroupMemberRepository.existsByGroupIdAndUserId(groupId, userId)
    }

    @Transactional
    override fun deleteByGroupIdAndUserId(groupId: UUID, userId: UUID) {
        springDataGroupMemberRepository.deleteByGroupIdAndUserId(groupId, userId)
    }

    @Transactional
    override fun deleteAllByGroupId(groupId: UUID) {
        springDataGroupMemberRepository.deleteAllByGroupId(groupId)
    }

    @Transactional
    override fun deleteAllByUserId(userId: UUID) {
        springDataGroupMemberRepository.deleteAllByUserId(userId)
    }
}
