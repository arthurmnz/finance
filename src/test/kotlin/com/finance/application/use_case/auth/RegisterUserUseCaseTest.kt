package com.finance.application.use_case.auth

import com.finance.application.dto.auth.RegisterUserRequest
import com.finance.application.mapper.UserDtoMapper
import com.finance.application.port.PasswordEncoderPort
import com.finance.application.service.CategorySeedService
import com.finance.application.service.DefaultCategoryCatalog
import com.finance.application.service.InMemoryUserRepository
import com.finance.application.service.UserService
import com.finance.domain.entity.CategoryEntity
import com.finance.domain.enum.CategoryLevel
import com.finance.domain.exception.UserAlreadyExistsException
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.TransactionRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RegisterUserUseCaseTest {

    private lateinit var userRepository: InMemoryUserRepository
    private lateinit var categoryRepository: InMemoryRegisterCategoryRepository
    private lateinit var categorySeedService: CategorySeedService
    private lateinit var userService: UserService
    private lateinit var passwordEncoder: FakePasswordEncoder
    private lateinit var userDtoMapper: UserDtoMapper
    private lateinit var registerUserUseCase: RegisterUserUseCase

    @BeforeEach
    fun setup() {
        userRepository = InMemoryUserRepository()
        categoryRepository = InMemoryRegisterCategoryRepository()
        categorySeedService = CategorySeedService(
            categoryRepository = categoryRepository,
            transactionRepository = mock(TransactionRepository::class.java),
            userRepository = userRepository
        )
        userService = UserService(userRepository)
        passwordEncoder = FakePasswordEncoder()
        userDtoMapper = UserDtoMapper()
        registerUserUseCase = RegisterUserUseCase(
            userRepository = userRepository,
            userService = userService,
            passwordEncoder = passwordEncoder,
            userDtoMapper = userDtoMapper,
            categorySeedService = categorySeedService
        )
    }

    @Test
    fun `should register user successfully with strong password and seed default categories`() {
        val request = RegisterUserRequest(
            firstName = "Arthur",
            lastName = "Menezes",
            email = "arthur@finance.com",
            password = "SecretPassword123!"
        )

        val response = registerUserUseCase.execute(request)

        assertNotNull(response.id)
        assertEquals("Arthur", response.firstName)
        assertEquals("Menezes", response.lastName)
        assertEquals("arthur@finance.com", response.email)

        val savedUser = userRepository.findById(response.id)
        assertNotNull(savedUser)
        assertTrue(savedUser.passwordHash.value.startsWith("hashed_"))

        val seeded = categoryRepository.findByUserId(response.id)
        val expectedGenerals = DefaultCategoryCatalog.generals.size
        val expectedSpecifics = DefaultCategoryCatalog.generals.sumOf { it.specifics.size }
        assertEquals(expectedGenerals, seeded.count { it.level == CategoryLevel.GENERAL })
        assertEquals(expectedSpecifics, seeded.count { it.level == CategoryLevel.SPECIFIC })
        assertTrue(seeded.all { it.isDefault && it.systemKey != null })
    }

    @Test
    fun `should fail when email is already registered`() {
        val request = RegisterUserRequest(
            firstName = "Arthur",
            lastName = "Menezes",
            email = "arthur@finance.com",
            password = "SecretPassword123!"
        )

        registerUserUseCase.execute(request)

        assertThrows<UserAlreadyExistsException> {
            registerUserUseCase.execute(request)
        }
    }

    @Test
    fun `should fail when password does not meet strength requirements`() {
        // Less than 8 chars
        assertThrows<IllegalArgumentException> {
            registerUserUseCase.execute(
                RegisterUserRequest("Arthur", "Menezes", "test1@finance.com", "Ab1!")
            )
        }

        // Missing uppercase
        assertThrows<IllegalArgumentException> {
            registerUserUseCase.execute(
                RegisterUserRequest("Arthur", "Menezes", "test2@finance.com", "secret123!")
            )
        }

        // Missing lowercase
        assertThrows<IllegalArgumentException> {
            registerUserUseCase.execute(
                RegisterUserRequest("Arthur", "Menezes", "test3@finance.com", "SECRET123!")
            )
        }

        // Missing digit
        assertThrows<IllegalArgumentException> {
            registerUserUseCase.execute(
                RegisterUserRequest("Arthur", "Menezes", "test4@finance.com", "SecretPassword!")
            )
        }

        // Missing special character
        assertThrows<IllegalArgumentException> {
            registerUserUseCase.execute(
                RegisterUserRequest("Arthur", "Menezes", "test5@finance.com", "SecretPassword123")
            )
        }
    }
}

class FakePasswordEncoder : PasswordEncoderPort {
    override fun encode(rawPassword: String): String = "hashed_$rawPassword"
    override fun matches(rawPassword: String, encodedPassword: String): Boolean =
        encodedPassword == "hashed_$rawPassword"
}

private class InMemoryRegisterCategoryRepository : CategoryRepository {
    private val store = linkedMapOf<UUID, CategoryEntity>()

    override fun save(category: CategoryEntity): CategoryEntity {
        store[category.id] = category
        return category
    }

    override fun saveAll(categories: List<CategoryEntity>): List<CategoryEntity> {
        categories.forEach { store[it.id] = it }
        return categories
    }

    override fun findById(id: UUID): CategoryEntity? = store[id]
    override fun findByIdIn(ids: Collection<UUID>): List<CategoryEntity> = ids.mapNotNull { store[it] }
    override fun findByUserId(userId: UUID): List<CategoryEntity> = store.values.filter { it.userId == userId }
    override fun findByUserIdAndLevel(userId: UUID, level: CategoryLevel): List<CategoryEntity> =
        store.values.filter { it.userId == userId && it.level == level }
    override fun findByGroupId(groupId: UUID): List<CategoryEntity> = store.values.filter { it.groupId == groupId }
    override fun findByGroupIdAndLevel(groupId: UUID, level: CategoryLevel): List<CategoryEntity> =
        store.values.filter { it.groupId == groupId && it.level == level }
    override fun findByParentId(parentId: UUID): List<CategoryEntity> = store.values.filter { it.parentId == parentId }
    override fun existsByParentId(parentId: UUID): Boolean = store.values.any { it.parentId == parentId }
    override fun findByHierarchyMigratedFalse(): List<CategoryEntity> = store.values.filter { !it.hierarchyMigrated }
    override fun delete(id: UUID) { store.remove(id) }
}
