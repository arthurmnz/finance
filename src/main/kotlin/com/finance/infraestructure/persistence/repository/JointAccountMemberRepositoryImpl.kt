package com.finance.infraestructure.persistence.repository

import com.finance.domain.entity.JointAccountMemberEntity
import com.finance.domain.repository.JointAccountMemberRepository
import com.finance.infraestructure.persistence.mapper.JointAccountMemberMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
class JointAccountMemberRepositoryImpl(
    private val springDataJointAccountMemberRepository: SpringDataJointAccountMemberRepository,
    private val mapper: JointAccountMemberMapper
) : JointAccountMemberRepository {

    @Transactional
    override fun save(member: JointAccountMemberEntity): JointAccountMemberEntity {
        val saved = springDataJointAccountMemberRepository.save(mapper.toJpaEntity(member))
        return mapper.toDomain(saved)
    }

    @Transactional
    override fun saveAll(members: List<JointAccountMemberEntity>): List<JointAccountMemberEntity> {
        if (members.isEmpty()) return emptyList()
        return springDataJointAccountMemberRepository.saveAll(members.map { mapper.toJpaEntity(it) })
            .map { mapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findAllByAccountId(accountId: UUID): List<JointAccountMemberEntity> {
        return springDataJointAccountMemberRepository.findAllByAccountId(accountId)
            .map { mapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findAllByAccountIdIn(accountIds: Collection<UUID>): List<JointAccountMemberEntity> {
        if (accountIds.isEmpty()) return emptyList()
        return springDataJointAccountMemberRepository.findAllByAccountIdIn(accountIds)
            .map { mapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findAllByUserId(userId: UUID): List<JointAccountMemberEntity> {
        return springDataJointAccountMemberRepository.findAllByUserId(userId)
            .map { mapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun existsByAccountIdAndUserId(accountId: UUID, userId: UUID): Boolean {
        return springDataJointAccountMemberRepository.existsByAccountIdAndUserId(accountId, userId)
    }

    @Transactional
    override fun deleteByAccountIdAndUserId(accountId: UUID, userId: UUID) {
        springDataJointAccountMemberRepository.deleteByAccountIdAndUserId(accountId, userId)
    }

    @Transactional
    override fun deleteAllByAccountId(accountId: UUID) {
        springDataJointAccountMemberRepository.deleteAllByAccountId(accountId)
    }

    @Transactional
    override fun deleteAllByAccountIdInAndUserId(accountIds: Collection<UUID>, userId: UUID) {
        if (accountIds.isEmpty()) return
        springDataJointAccountMemberRepository.deleteAllByAccountIdInAndUserId(accountIds, userId)
    }

    @Transactional
    override fun deleteAllByUserId(userId: UUID) {
        springDataJointAccountMemberRepository.deleteAllByUserId(userId)
    }
}
