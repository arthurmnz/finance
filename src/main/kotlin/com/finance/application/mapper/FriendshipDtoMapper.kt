package com.finance.application.mapper

import com.finance.application.dto.friendship.FriendshipResponse
import com.finance.application.dto.friendship.FriendshipUserSummary
import com.finance.application.service.UserService
import com.finance.domain.entity.FriendshipEntity
import com.finance.domain.entity.UserEntity
import org.springframework.stereotype.Component

@Component
class FriendshipDtoMapper(
    private val userService: UserService,
) {

    fun toFriendshipResponse(entity: FriendshipEntity): FriendshipResponse {
        val requester = userService.findById(entity.requesterId)
        val addressee = userService.findById(entity.addresseeId)
        return toFriendshipResponse(entity, requester, addressee)
    }

    fun toFriendshipResponse(
        entity: FriendshipEntity,
        requester: UserEntity,
        addressee: UserEntity,
    ): FriendshipResponse {
        return FriendshipResponse(
            id = entity.id,
            requester = toUserSummary(requester),
            addressee = toUserSummary(addressee),
            status = entity.status,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }

    fun toFriendshipResponseList(entities: List<FriendshipEntity>): List<FriendshipResponse> {
        return entities.map { toFriendshipResponse(it) }
    }

    private fun toUserSummary(user: UserEntity): FriendshipUserSummary {
        return FriendshipUserSummary(
            id = user.id,
            firstName = user.firstName.value,
            lastName = user.lastName.value,
            email = user.email.normalized,
        )
    }
}
