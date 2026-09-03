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
class AcceptFriendRequestUseCase(
    private val friendshipRepository: FriendshipRepository,
    private val friendshipDtoMapper: FriendshipDtoMapper,
) {

    @Transactional
    fun execute(friendshipId: UUID, authenticatedUserId: UUID): FriendshipResponse {
        val friendship = friendshipRepository.findById(friendshipId)
            ?: throw FriendshipNotFoundException()

        // Apenas o destinatário pode aceitar a solicitação
        if (friendship.addresseeId != authenticatedUserId) {
            throw AccessDeniedException("Apenas o destinatário da solicitação pode aceitá-la.")
        }

        friendship.accept()

        val saved = friendshipRepository.save(friendship)
        return friendshipDtoMapper.toFriendshipResponse(saved)
    }
}
