package com.finance.application.use_case.friendship

import com.finance.application.dto.friendship.FriendshipResponse
import com.finance.application.mapper.FriendshipDtoMapper
import com.finance.domain.enum.FriendshipStatus
import com.finance.domain.repository.FriendshipRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ListPendingSentRequestsUseCase(
    private val friendshipRepository: FriendshipRepository,
    private val friendshipDtoMapper: FriendshipDtoMapper,
) {

    @Transactional(readOnly = true)
    fun execute(authenticatedUserId: UUID): List<FriendshipResponse> {
        val friendships = friendshipRepository.findAllByRequesterIdAndStatus(
            requesterId = authenticatedUserId,
            status = FriendshipStatus.PENDING,
        )
        return friendshipDtoMapper.toFriendshipResponseList(friendships)
    }
}
