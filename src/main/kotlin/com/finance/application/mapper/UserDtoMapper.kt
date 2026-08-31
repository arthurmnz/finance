package com.finance.application.mapper

import com.finance.application.dto.auth.RegisterUserResponse
import com.finance.application.dto.user.UserResponse
import com.finance.domain.entity.UserEntity
import org.springframework.stereotype.Component

@Component
class UserDtoMapper {

    fun toUserResponse(user: UserEntity): UserResponse {
        return UserResponse(
            id = user.id,
            firstName = user.firstName.value,
            lastName = user.lastName.value,
            email = user.email.normalized,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
        )
    }

    fun toRegisterResponse(user: UserEntity): RegisterUserResponse {
        return RegisterUserResponse(
            id = user.id,
            firstName = user.firstName.value,
            lastName = user.lastName.value,
            email = user.email.normalized,
            createdAt = user.createdAt,
        )
    }
}
