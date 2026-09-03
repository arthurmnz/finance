package com.finance.infraestructure.persistence.repository

import com.finance.domain.enum.FriendshipStatus
import com.finance.infraestructure.persistence.entity.FriendshipJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SpringDataFriendshipRepository : JpaRepository<FriendshipJpaEntity, UUID> {

    fun findByRequesterIdAndAddresseeId(requesterId: UUID, addresseeId: UUID): FriendshipJpaEntity?

    @Query(
        """
        SELECT f FROM FriendshipJpaEntity f
        WHERE (f.requesterId = :userIdA AND f.addresseeId = :userIdB)
           OR (f.requesterId = :userIdB AND f.addresseeId = :userIdA)
        """
    )
    fun findBetweenUsers(
        @Param("userIdA") userIdA: UUID,
        @Param("userIdB") userIdB: UUID,
    ): FriendshipJpaEntity?

    @Query(
        """
        SELECT f FROM FriendshipJpaEntity f
        WHERE f.requesterId = :userId OR f.addresseeId = :userId
        """
    )
    fun findAllByUserId(@Param("userId") userId: UUID): List<FriendshipJpaEntity>

    @Query(
        """
        SELECT f FROM FriendshipJpaEntity f
        WHERE (f.requesterId = :userId OR f.addresseeId = :userId)
          AND f.status = :status
        """
    )
    fun findAllByUserIdAndStatus(
        @Param("userId") userId: UUID,
        @Param("status") status: FriendshipStatus,
    ): List<FriendshipJpaEntity>

    fun findAllByRequesterIdAndStatus(
        requesterId: UUID,
        status: FriendshipStatus,
    ): List<FriendshipJpaEntity>

    fun findAllByAddresseeIdAndStatus(
        addresseeId: UUID,
        status: FriendshipStatus,
    ): List<FriendshipJpaEntity>
}
