package com.finance.infraestructure.persistence.repository

import com.finance.infraestructure.persistence.entity.JointAccountMemberJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SpringDataJointAccountMemberRepository : JpaRepository<JointAccountMemberJpaEntity, UUID> {
    fun findAllByAccountId(accountId: UUID): List<JointAccountMemberJpaEntity>
    fun findAllByAccountIdIn(accountIds: Collection<UUID>): List<JointAccountMemberJpaEntity>
    fun findAllByUserId(userId: UUID): List<JointAccountMemberJpaEntity>
    fun existsByAccountIdAndUserId(accountId: UUID, userId: UUID): Boolean
    fun deleteByAccountIdAndUserId(accountId: UUID, userId: UUID)
    fun deleteAllByAccountId(accountId: UUID)
    fun deleteAllByAccountIdInAndUserId(accountIds: Collection<UUID>, userId: UUID)
    fun deleteAllByUserId(userId: UUID)
}
