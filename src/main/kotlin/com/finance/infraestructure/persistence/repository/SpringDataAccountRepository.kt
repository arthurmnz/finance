package com.finance.infraestructure.persistence.repository

import com.finance.infraestructure.persistence.entity.AccountJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SpringDataAccountRepository : JpaRepository<AccountJpaEntity, UUID> {
    fun findAllByIdIn(ids: Collection<UUID>): List<AccountJpaEntity>
    fun findAllByOwnerId(ownerId: UUID): List<AccountJpaEntity>
    fun findAllByGroupId(groupId: UUID): List<AccountJpaEntity>
    fun findAllByGroupIdIn(groupIds: Collection<UUID>): List<AccountJpaEntity>
    fun findAllByGroupIdAndIsJointTrue(groupId: UUID): List<AccountJpaEntity>
    fun existsByOwnerIdAndGroupIdIsNotNull(ownerId: UUID): Boolean
    fun existsByOwnerIdAndGroupId(ownerId: UUID, groupId: UUID): Boolean
    fun deleteAllByOwnerId(ownerId: UUID)
    fun deleteAllByGroupIdAndIsJointTrue(groupId: UUID)
}
