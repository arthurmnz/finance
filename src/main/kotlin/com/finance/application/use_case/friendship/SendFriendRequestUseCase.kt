package com.finance.application.use_case.friendship

import com.finance.application.dto.friendship.FriendshipResponse
import com.finance.application.dto.friendship.SendFriendRequestRequest
import com.finance.application.mapper.FriendshipDtoMapper
import com.finance.application.service.UserService
import com.finance.domain.entity.FriendshipEntity
import com.finance.domain.exception.FriendshipAlreadyExistsException
import com.finance.domain.repository.FriendshipRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SendFriendRequestUseCase(
    private val friendshipRepository: FriendshipRepository,
    private val userService: UserService,
    private val friendshipDtoMapper: FriendshipDtoMapper,
) {

    @Transactional
    fun execute(requesterId: UUID, request: SendFriendRequestRequest): FriendshipResponse {
        // Resolve o destinatário pelo e-mail
        val addressee = userService.findByEmail(request.addresseeEmail)

        require(requesterId != addressee.id) { "Você não pode enviar uma solicitação de amizade para si mesmo." }

        // Verifica se já existe alguma relação entre os dois usuários
        val existing = friendshipRepository.findBetweenUsers(requesterId, addressee.id)
        if (existing != null) {
            throw FriendshipAlreadyExistsException("Já existe uma relação de amizade com status '${existing.status}' entre esses usuários.")
        }

        val friendship = FriendshipEntity(
            requesterId = requesterId,
            addresseeId = addressee.id,
        )

        val saved = friendshipRepository.save(friendship)
        return friendshipDtoMapper.toFriendshipResponse(saved)
    }
}
