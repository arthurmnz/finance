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

        val hashedPassword = passwordEncoder.encode(request.password)

        val newUser = UserEntity(
            firstName = firstName,
            lastName = lastName,
            email = email,
            passwordHash = PasswordHash(hashedPassword),
        )

        val savedUser = userRepository.save(newUser)
        return userDtoMapper.toRegisterResponse(savedUser)
    }
}
