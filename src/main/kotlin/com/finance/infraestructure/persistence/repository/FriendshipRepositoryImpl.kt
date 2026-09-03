package com.finance.infraestructure.persistence.repository

import com.finance.domain.entity.FriendshipEntity
import com.finance.domain.enum.FriendshipStatus
import com.finance.domain.repository.FriendshipRepository
import com.finance.infraestructure.persistence.mapper.FriendshipMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
class FriendshipRepositoryImpl(
    private val springDataFriendshipRepository: SpringDataFriendshipRepository,
    private val friendshipMapper: FriendshipMapper,
) : FriendshipRepository {

    @Transactional
    override fun save(friendship: FriendshipEntity): FriendshipEntity {
        val jpaEntity = friendshipMapper.toJpaEntity(friendship)
        val saved = springDataFriendshipRepository.save(jpaEntity)
        return friendshipMapper.toDomain(saved)
    }

    @Transactional(readOnly = true)
    override fun findById(id: UUID): FriendshipEntity? {
        return springDataFriendshipRepository.findById(id)
            .map { friendshipMapper.toDomain(it) }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByRequesterAndAddressee(requesterId: UUID, addresseeId: UUID): FriendshipEntity? {
        return springDataFriendshipRepository
            .findByRequesterIdAndAddresseeId(requesterId, addresseeId)
            ?.let { friendshipMapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findBetweenUsers(userIdA: UUID, userIdB: UUID): FriendshipEntity? {
        return springDataFriendshipRepository
            .findBetweenUsers(userIdA, userIdB)
            ?.let { friendshipMapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findAllByUserId(userId: UUID): List<FriendshipEntity> {
        return springDataFriendshipRepository
            .findAllByUserId(userId)
            .map { friendshipMapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findAllByUserIdAndStatus(userId: UUID, status: FriendshipStatus): List<FriendshipEntity> {
        return springDataFriendshipRepository
            .findAllByUserIdAndStatus(userId, status)
            .map { friendshipMapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findAllByRequesterIdAndStatus(requesterId: UUID, status: FriendshipStatus): List<FriendshipEntity> {
        return springDataFriendshipRepository
            .findAllByRequesterIdAndStatus(requesterId, status)
            .map { friendshipMapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findAllByAddresseeIdAndStatus(addresseeId: UUID, status: FriendshipStatus): List<FriendshipEntity> {
        return springDataFriendshipRepository
            .findAllByAddresseeIdAndStatus(addresseeId, status)
            .map { friendshipMapper.toDomain(it) }
    }
}
