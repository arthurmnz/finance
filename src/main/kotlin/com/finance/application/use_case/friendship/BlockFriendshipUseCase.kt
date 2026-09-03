package com.finance.application.use_case.friendship

import com.finance.application.dto.friendship.FriendshipResponse
import com.finance.application.mapper.FriendshipDtoMapper
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.FriendshipNotFoundException
import com.finance.domain.repository.FriendshipRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BlockFriendshipUseCase(
    private val friendshipRepository: FriendshipRepository,
    private val friendshipDtoMapper: FriendshipDtoMapper,
) {

    @Transactional
    fun execute(friendshipId: UUID, authenticatedUserId: UUID): FriendshipResponse {
        val friendship = friendshipRepository.findById(friendshipId)
            ?: throw FriendshipNotFoundException()

        // Apenas participantes da relação podem bloquear
        if (friendship.requesterId != authenticatedUserId && friendship.addresseeId != authenticatedUserId) {
            throw AccessDeniedException("Você não faz parte dessa relação de amizade.")
        }

        friendship.block()

        val saved = friendshipRepository.save(friendship)
        return friendshipDtoMapper.toFriendshipResponse(saved)
    }
}
