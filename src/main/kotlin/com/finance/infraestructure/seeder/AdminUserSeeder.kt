package com.finance.infraestructure.seeder

import com.finance.application.port.PasswordEncoderPort
import com.finance.domain.entity.UserEntity
import com.finance.domain.enum.UserRole
import com.finance.domain.repository.UserRepository
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class AdminUserSeeder(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoderPort,
    @Value("\${app.admin.email:}") private val adminEmail: String,
    @Value("\${app.admin.password:}") private val adminPassword: String,
    @Value("\${app.admin.first-name:Admin}") private val adminFirstName: String,
    @Value("\${app.admin.last-name:Sistema}") private val adminLastName: String,
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String) {
        val emailStr = adminEmail.trim()
        val passwordStr = adminPassword.trim()

        if (emailStr.isBlank() || passwordStr.isBlank()) {
            logger.info("Admin seeder ignorado: ADMIN_EMAIL ou ADMIN_PASSWORD não configurados.")
            return
        }

        if (!Email.isValid(emailStr)) {
            logger.error("Admin seeder: email '$emailStr' inválido. Nenhum admin criado.")
            return
        }

        val email = Email(emailStr)
        val existingUser = userRepository.findByEmail(email)

        if (existingUser != null) {
            if (existingUser.role == UserRole.ADMIN) {
                logger.info("Usuário administrador '$emailStr' já existe no banco de dados.")
            } else {
                logger.warn("O email '$emailStr' já está cadastrado no banco. Nenhuma alteração foi feita. Utilize outro email em ADMIN_EMAIL para criar um novo administrador.")
            }
            return
        }

        try {
            val firstName = Name(adminFirstName.ifBlank { "Admin" })
            val lastName = Name(adminLastName.ifBlank { "Sistema" })
            val encoded = passwordEncoder.encode(passwordStr)
            val passwordHash = PasswordHash(encoded)

            val adminUser = UserEntity(
                firstName = firstName,
                lastName = lastName,
                email = email,
                passwordHash = passwordHash,
                role = UserRole.ADMIN,
                isActive = true,
            )

            userRepository.save(adminUser)
            logger.info("Administrador inicial cadastrado com sucesso: $emailStr")
        } catch (ex: Exception) {
            logger.error("Erro ao cadastrar administrador inicial pelo seeder: ${ex.message}", ex)
        }
    }
}
