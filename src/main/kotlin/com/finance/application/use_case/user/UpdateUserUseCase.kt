package com.finance.application.use_case.user

import com.finance.application.dto.user.UpdateUserRequest
import com.finance.application.dto.user.UserResponse
import com.finance.application.mapper.UserDtoMapper
import com.finance.application.service.UserService
import com.finance.domain.repository.UserRepository
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UpdateUserUseCase(
    private val userRepository: UserRepository,
    private val userService: UserService,
    private val userDtoMapper: UserDtoMapper,
) {

    @Transactional
    fun execute(
        userId: UUID,
        request: UpdateUserRequest,
        authenticatedUserId: UUID? = null,
    ): UserResponse {
        if (authenticatedUserId != null) {
            userService.validateUserOwnership(authenticatedUserId, userId)
        }

        val user = userService.findById(userId)

        if (request.firstName != null || request.lastName != null) {
            val newFirstName = request.firstName?.let { Name(it) } ?: user.firstName
            val newLastName = request.lastName?.let { Name(it) } ?: user.lastName
            user.updateName(newFirstName, newLastName)
        }

        if (request.email != null) {
            val newEmail = Email(request.email)
            if (newEmail.normalized != user.email.normalized) {
                userService.checkEmailAvailability(newEmail)
                user.updateEmail(newEmail)
            }
        }

        val updatedUser = userRepository.save(user)
        return userDtoMapper.toUserResponse(updatedUser)
    }
}
