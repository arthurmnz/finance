package com.finance.application.use_case.admin

import com.finance.application.dto.user.UserResponse
import com.finance.application.mapper.UserDtoMapper
import com.finance.domain.exception.UserNotFoundException
import com.finance.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ToggleUserStatusByAdminUseCase(
    private val userRepository: UserRepository,
    private val userDtoMapper: UserDtoMapper,
) {
    @Transactional
    fun execute(authenticatedAdminId: UUID, targetUserId: UUID): UserResponse {
        if (authenticatedAdminId == targetUserId) {
            throw IllegalArgumentException("Você não pode bloquear sua própria conta de administrador.")
        }

        val user = userRepository.findById(targetUserId)
            ?: throw UserNotFoundException()

        user.toggleActive()
        val updated = userRepository.save(user)

        return userDtoMapper.toUserResponse(updated)
    }
}
