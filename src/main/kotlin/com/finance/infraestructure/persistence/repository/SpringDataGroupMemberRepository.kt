package com.finance.infraestructure.persistence.repository

import com.finance.domain.enum.GroupRole
import com.finance.infraestructure.persistence.entity.GroupMemberJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SpringDataGroupMemberRepository : JpaRepository<GroupMemberJpaEntity, UUID> {

    fun findByGroupIdAndUserId(groupId: UUID, userId: UUID): GroupMemberJpaEntity?

    fun findAllByGroupId(groupId: UUID): List<GroupMemberJpaEntity>

    fun findAllByUserId(userId: UUID): List<GroupMemberJpaEntity>

    fun countByGroupId(groupId: UUID): Int

    fun existsByGroupIdAndUserId(groupId: UUID, userId: UUID): Boolean

    fun deleteByGroupIdAndUserId(groupId: UUID, userId: UUID)

    fun deleteAllByGroupId(groupId: UUID)

    fun deleteAllByUserId(userId: UUID)

    @Query(
        """
        SELECT m FROM GroupMemberJpaEntity m
        WHERE m.groupId = :groupId
          AND m.role = :role
        ORDER BY m.joinedAt ASC
        LIMIT 1
        """
    )
    fun findOldestByGroupIdAndRole(
        @Param("groupId") groupId: UUID,
        @Param("role") role: GroupRole,
    ): GroupMemberJpaEntity?

    @Query(
        """
        SELECT m FROM GroupMemberJpaEntity m
        WHERE m.groupId = :groupId
          AND m.role <> 'OWNER'
        ORDER BY m.joinedAt ASC
        LIMIT 1
        """
    )
    fun findOldestNonOwnerByGroupId(@Param("groupId") groupId: UUID): GroupMemberJpaEntity?
}
