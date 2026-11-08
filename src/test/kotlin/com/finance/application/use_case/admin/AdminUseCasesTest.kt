package com.finance.application.use_case.admin

import com.finance.application.mapper.UserDtoMapper
import com.finance.application.service.InMemoryUserRepository
import com.finance.application.use_case.auth.FakePasswordEncoder
import com.finance.domain.entity.UserEntity
import com.finance.domain.enum.UserRole
import com.finance.domain.exception.UserNotFoundException
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import com.finance.infraestructure.seeder.AdminUserSeeder
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import jakarta.persistence.TypedQuery
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdminUseCasesTest {

    private lateinit var userRepository: InMemoryUserRepository
    private lateinit var userDtoMapper: UserDtoMapper
    private lateinit var entityManager: EntityManager
    private lateinit var passwordEncoder: FakePasswordEncoder

    private lateinit var toggleUserStatusByAdminUseCase: ToggleUserStatusByAdminUseCase
    private lateinit var deleteUserByAdminUseCase: DeleteUserByAdminUseCase

    @BeforeEach
    fun setup() {
        userRepository = InMemoryUserRepository()
        userDtoMapper = UserDtoMapper()
        entityManager = mock(EntityManager::class.java)
        passwordEncoder = FakePasswordEncoder()

        toggleUserStatusByAdminUseCase = ToggleUserStatusByAdminUseCase(userRepository, userDtoMapper)
        deleteUserByAdminUseCase = DeleteUserByAdminUseCase(userRepository, entityManager)
    }

    @Test
    fun `toggleStatus should throw IllegalArgumentException when admin toggles self`() {
        val adminId = UUID.randomUUID()

        val exception = assertThrows<IllegalArgumentException> {
            toggleUserStatusByAdminUseCase.execute(adminId, adminId)
        }
        assertEquals("Você não pode bloquear sua própria conta de administrador.", exception.message)
    }

    @Test
    fun `toggleStatus should throw UserNotFoundException when user does not exist`() {
        val adminId = UUID.randomUUID()
        val targetId = UUID.randomUUID()

        assertThrows<UserNotFoundException> {
            toggleUserStatusByAdminUseCase.execute(adminId, targetId)
        }
    }

    @Test
    fun `toggleStatus should toggle active status and return updated UserResponse`() {
        val adminId = UUID.randomUUID()
        val targetUser = UserEntity(
            firstName = Name("Regular"),
            lastName = Name("User"),
            email = Email("regular@finance.com"),
            passwordHash = PasswordHash("hash"),
            role = UserRole.USER,
            isActive = true
        )
        userRepository.save(targetUser)

        val result = toggleUserStatusByAdminUseCase.execute(adminId, targetUser.id)

        assertFalse(result.isActive)
        val saved = userRepository.findById(targetUser.id)
        assertNotNull(saved)
        assertFalse(saved.isActive)

        // Toggle back to active
        val resultBack = toggleUserStatusByAdminUseCase.execute(adminId, targetUser.id)
        assertTrue(resultBack.isActive)
        assertTrue(userRepository.findById(targetUser.id)!!.isActive)
    }

    @Test
    fun `deleteUser should throw IllegalArgumentException when admin deletes self`() {
        val adminId = UUID.randomUUID()

        val exception = assertThrows<IllegalArgumentException> {
            deleteUserByAdminUseCase.execute(adminId, adminId)
        }
        assertEquals("Você não pode excluir sua própria conta de administrador.", exception.message)
    }

    @Test
    fun `deleteUser should throw UserNotFoundException when user does not exist`() {
        val adminId = UUID.randomUUID()
        val targetId = UUID.randomUUID()

        assertThrows<UserNotFoundException> {
            deleteUserByAdminUseCase.execute(adminId, targetId)
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `deleteUser should cascade delete dependencies and delete user`() {
        val adminId = UUID.randomUUID()
        val targetUser = UserEntity(
            firstName = Name("To"),
            lastName = Name("Delete"),
            email = Email("delete@finance.com"),
            passwordHash = PasswordHash("hash"),
            role = UserRole.USER,
            isActive = true
        )
        userRepository.save(targetUser)

        val typedQuery = mock(TypedQuery::class.java) as TypedQuery<UUID>
        `when`(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery)
        `when`(typedQuery.resultList).thenReturn(emptyList())

        `when`(entityManager.createQuery(anyString(), eq(UUID::class.java))).thenReturn(typedQuery)

        val query = mock(Query::class.java)
        `when`(query.setParameter(anyString(), any())).thenReturn(query)
        `when`(query.executeUpdate()).thenReturn(1)
        `when`(entityManager.createQuery(anyString())).thenReturn(query)

        deleteUserByAdminUseCase.execute(adminId, targetUser.id)

        assertNull(userRepository.findById(targetUser.id))
    }

    @Test
    fun `adminSeeder should skip when admin user already exists`() {
        val email = "admin@finance.com"
        val existingAdmin = UserEntity(
            firstName = Name("Admin"),
            lastName = Name("Finance"),
            email = Email(email),
            passwordHash = PasswordHash("hashed_initial_secret"),
            role = UserRole.ADMIN
        )
        userRepository.save(existingAdmin)

        val seeder = AdminUserSeeder(
            userRepository = userRepository,
            passwordEncoder = passwordEncoder,
            adminEmail = email,
            adminPassword = "SecretPassword123",
            adminFirstName = "Admin",
            adminLastName = "Finance"
        )

        seeder.run()

        val userInDb = userRepository.findByEmail(Email(email))
        assertNotNull(userInDb)
        assertEquals("hashed_initial_secret", userInDb.passwordHash.value)
        assertEquals(1, userRepository.findAll().size)
    }

    @Test
    fun `adminSeeder should create admin user when email not registered`() {
        val email = "newadmin@finance.com"

        val seeder = AdminUserSeeder(
            userRepository = userRepository,
            passwordEncoder = passwordEncoder,
            adminEmail = email,
            adminPassword = "SecretPassword123",
            adminFirstName = "Super",
            adminLastName = "Admin"
        )

        seeder.run()

        val userInDb = userRepository.findByEmail(Email(email))
        assertNotNull(userInDb)
        assertEquals("Super", userInDb.firstName.value)
        assertEquals("Admin", userInDb.lastName.value)
        assertEquals(UserRole.ADMIN, userInDb.role)
        assertTrue(userInDb.isActive)
        assertEquals("hashed_SecretPassword123", userInDb.passwordHash.value)
    }
}
