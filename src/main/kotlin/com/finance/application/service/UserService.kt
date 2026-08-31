package com.finance.application.service

import com.finance.domain.entity.UserEntity
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.UserAlreadyExistsException
import com.finance.domain.exception.UserNotFoundException
import com.finance.domain.repository.UserRepository
import com.finance.domain.value_object.Email
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
) {

    // ==========================================
    // FINDER HELPER FUNCTIONS
    // ==========================================

    fun findById(id: UUID): UserEntity {
        return userRepository.findById(id)
            ?: throw UserNotFoundException("Usuário não encontrado com o ID: $id")
    }

    fun findByEmail(email: Email): UserEntity {
        return userRepository.findByEmail(email)
            ?: throw UserNotFoundException("Usuário não encontrado com o e-mail: ${email.normalized}")
    }

    fun findByEmail(email: String): UserEntity {
        return findByEmail(Email(email))
    }

    fun findOptionalById(id: UUID): UserEntity? {
        return userRepository.findById(id)
    }

    fun findOptionalByEmail(email: Email): UserEntity? {
        return userRepository.findByEmail(email)
    }

    fun existsByEmail(email: Email): Boolean {
        return userRepository.existsByEmail(email)
    }

    fun checkEmailAvailability(email: Email) {
        if (userRepository.existsByEmail(email)) {
            throw UserAlreadyExistsException("O e-mail '${email.normalized}' já está em uso.")
        }
    }

    fun findAll(): List<UserEntity> {
        return userRepository.findAll()
    }

    // ==========================================
    // PERMISSION HELPER FUNCTIONS
    // ==========================================

    fun validateUserOwnership(authenticatedUserId: UUID, targetUserId: UUID) {
        if (authenticatedUserId != targetUserId) {
            throw AccessDeniedException("Você não tem permissão para manipular os dados deste usuário.")
        }
    }

    fun canModifyUser(authenticatedUserId: UUID, targetUserId: UUID): Boolean {
        return authenticatedUserId == targetUserId
    }

    fun requirePermission(condition: Boolean, message: String = "Acesso negado para esta operação.") {
        if (!condition) {
            throw AccessDeniedException(message)
        }
    }
}
