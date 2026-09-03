package com.finance.application.use_case.auth

import com.finance.application.dto.auth.RegisterUserRequest
import com.finance.application.dto.auth.RegisterUserResponse
import com.finance.application.mapper.UserDtoMapper
import com.finance.application.port.PasswordEncoderPort
import com.finance.application.service.UserService
import com.finance.domain.entity.UserEntity
import com.finance.domain.repository.UserRepository
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegisterUserUseCase(
    private val userRepository: UserRepository,
    private val userService: UserService,
    private val passwordEncoder: PasswordEncoderPort,
    private val userDtoMapper: UserDtoMapper,
) {

    @Transactional
    fun execute(request: RegisterUserRequest): RegisterUserResponse {
        val email = Email(request.email)
        val firstName = Name(request.firstName)
        val lastName = Name(request.lastName)

        userService.checkEmailAvailability(email)

        val validPassword = validatePasswordStrength(request.password)

        val hashedPassword = passwordEncoder.encode(validPassword)

        val newUser = UserEntity(
            firstName = firstName,
            lastName = lastName,
            email = email,
            passwordHash = PasswordHash(hashedPassword),
        )

        val savedUser = userRepository.save(newUser)
        return userDtoMapper.toRegisterResponse(savedUser)
    }

    private fun validatePasswordStrength(rawPassword: String): String {
        require(rawPassword.isNotBlank()) { "A senha não pode estar em branco." }
        require(rawPassword.length >= 8) { "A senha deve ter no mínimo 8 caracteres." }
        require(rawPassword.length <= 128) { "A senha deve ter no máximo 128 caracteres." }
        require(rawPassword.any { it.isUpperCase() }) { "A senha deve conter pelo menos uma letra maiúscula." }
        require(rawPassword.any { it.isLowerCase() }) { "A senha deve conter pelo menos uma letra minúscula." }
        require(rawPassword.any { it.isDigit() }) { "A senha deve conter pelo menos um número." }
        require(rawPassword.any { !it.isLetterOrDigit() }) { "A senha deve conter pelo menos um caractere especial." }
        return rawPassword
    }
}
