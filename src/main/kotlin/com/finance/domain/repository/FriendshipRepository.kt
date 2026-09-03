package com.finance.domain.repository

import com.finance.domain.entity.FriendshipEntity
import com.finance.domain.enum.FriendshipStatus
import java.util.UUID

interface FriendshipRepository {
    fun save(friendship: FriendshipEntity): FriendshipEntity
    fun findById(id: UUID): FriendshipEntity?
    fun findByRequesterAndAddressee(requesterId: UUID, addresseeId: UUID): FriendshipEntity?
    fun findBetweenUsers(userIdA: UUID, userIdB: UUID): FriendshipEntity?
    fun findAllByUserId(userId: UUID): List<FriendshipEntity>
    fun findAllByUserIdAndStatus(userId: UUID, status: FriendshipStatus): List<FriendshipEntity>
    fun findAllByRequesterIdAndStatus(requesterId: UUID, status: FriendshipStatus): List<FriendshipEntity>
    fun findAllByAddresseeIdAndStatus(addresseeId: UUID, status: FriendshipStatus): List<FriendshipEntity>
}
