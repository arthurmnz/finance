package com.finance.application.use_case.user

import com.finance.application.service.UserService
import com.finance.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DeleteUserUseCase(
    private val userRepository: UserRepository,
    private val userService: UserService,
) {

    @Transactional
    fun execute(userId: UUID, authenticatedUserId: UUID? = null) {
        if (authenticatedUserId != null) {
            userService.validateUserOwnership(authenticatedUserId, userId)
        }

        // Ensures user exists before deletion or throws UserNotFoundException
        userService.findById(userId)

        userRepository.deleteById(userId)
    }
}
