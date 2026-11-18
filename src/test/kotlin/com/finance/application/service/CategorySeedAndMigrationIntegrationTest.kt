package com.finance.application.service

import com.finance.application.use_case.category.DeleteCategoryUseCase
import com.finance.application.use_case.category.ListUserCategoriesUseCase
import com.finance.application.use_case.category.SeedDefaultCategoriesUseCase
import com.finance.domain.entity.AccountEntity
import com.finance.domain.entity.CategoryEntity
import com.finance.domain.entity.GroupEntity
import com.finance.domain.entity.GroupMemberEntity
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.entity.UserEntity
import com.finance.domain.enum.AccountType
import com.finance.domain.enum.CategoryLevel
import com.finance.domain.enum.CategoryType
import com.finance.domain.enum.GroupRole
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.repository.UserRepository
import com.finance.domain.value_object.Email
import com.finance.domain.value_object.Name
import com.finance.domain.value_object.PasswordHash
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
@Transactional
class CategorySeedAndMigrationIntegrationTest @Autowired constructor(
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val categorySeedService: CategorySeedService,
    private val seedDefaultCategoriesUseCase: SeedDefaultCategoriesUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    private val listUserCategoriesUseCase: ListUserCategoriesUseCase
) {

    @Test
    fun `should migrate legacy user and group categories idempotently and reassign general transactions`() {
        val user = userRepository.save(
            UserEntity(
                firstName = Name("Legacy"),
                lastName = Name("User"),
                email = Email("legacy.${UUID.randomUUID()}@finance.com"),
                passwordHash = PasswordHash("hash")
            )
        )
        val groupWithLegacy = groupRepository.save(GroupEntity(name = "Grupo Legado"))
        val emptyGroup = groupRepository.save(GroupEntity(name = "Grupo Vazio"))
        groupMemberRepository.save(GroupMemberEntity(groupId = groupWithLegacy.id, userId = user.id, role = GroupRole.OWNER))
        groupMemberRepository.save(GroupMemberEntity(groupId = emptyGroup.id, userId = user.id, role = GroupRole.OWNER))

        val account = accountRepository.save(
            AccountEntity(
                ownerId = user.id,
                name = "Conta Corrente",
                startBalance = 100000,
                type = AccountType.CHECKING,
                tax = null
            )
        )

        // Legacy personal category matching catalog GENERAL ("Moradia") with direct transaction
        val legacyMoradia = categoryRepository.save(
            CategoryEntity(
                name = "Moradia",
                description = "Antiga moradia",
                userId = user.id,
                groupId = null,
                type = CategoryType.EXPENSE,
                color = "#7D39EB",
                icon = "🏠",
                level = CategoryLevel.GENERAL,
                parentId = null,
                systemKey = null,
                isDefault = false,
                hierarchyMigrated = false
            )
        )
        // Legacy personal category matching catalog SPECIFIC ("Agua" sem acento)
        val legacyAgua = categoryRepository.save(
            CategoryEntity(
                name = "Agua",
                description = null,
                userId = user.id,
                groupId = null,
                type = CategoryType.EXPENSE,
                color = "#7D39EB",
                icon = "💧",
                level = CategoryLevel.GENERAL,
                parentId = null,
                systemKey = null,
                isDefault = false,
                hierarchyMigrated = false
            )
        )
        // Legacy custom personal category ("Pet Shop")
        val legacyPet = categoryRepository.save(
            CategoryEntity(
                name = "Pet Shop",
                description = null,
                userId = user.id,
                groupId = null,
                type = CategoryType.EXPENSE,
                color = "#112233",
                icon = "🐶",
                level = CategoryLevel.GENERAL,
                parentId = null,
                systemKey = null,
                isDefault = false,
                hierarchyMigrated = false
            )
        )

        // Legacy group category ("Churrasco")
        val legacyGroupCat = categoryRepository.save(
            CategoryEntity(
                name = "Churrasco",
                description = null,
                userId = null,
                groupId = groupWithLegacy.id,
                type = CategoryType.EXPENSE,
                color = "#FF6600",
                icon = "🥩",
                level = CategoryLevel.GENERAL,
                parentId = null,
                systemKey = null,
                isDefault = false,
                hierarchyMigrated = false
            )
        )

        val txOnMoradia = transactionRepository.save(
            TransactionEntity(
                title = "Reforma",
                amount = 50000,
                date = LocalDate.of(2026, 5, 1),
                responsibleId = user.id,
                accountId = account.id,
                type = TransactionType.EXPENSE,
                destinationAccountId = null,
                categoryId = legacyMoradia.id,
                status = TransactionStatus.COMPLETED
            )
        )
        val txOnAgua = transactionRepository.save(
            TransactionEntity(
                title = "Sabesp",
                amount = 8000,
                date = LocalDate.of(2026, 5, 2),
                responsibleId = user.id,
                accountId = account.id,
                type = TransactionType.EXPENSE,
                destinationAccountId = null,
                categoryId = legacyAgua.id,
                status = TransactionStatus.COMPLETED
            )
        )
        val txOnPet = transactionRepository.save(
            TransactionEntity(
                title = "Ração",
                amount = 12000,
                date = LocalDate.of(2026, 5, 3),
                responsibleId = user.id,
                accountId = account.id,
                type = TransactionType.EXPENSE,
                destinationAccountId = null,
                categoryId = legacyPet.id,
                status = TransactionStatus.COMPLETED
            )
        )

        // Run migration 1st time
        categorySeedService.migrateLegacyCategoriesAndTransactions()

        val userCats = categoryRepository.findByUserId(user.id)
        assertTrue(userCats.all { it.hierarchyMigrated })

        val migratedMoradia = categoryRepository.findById(legacyMoradia.id)!!
        assertEquals("MORADIA", migratedMoradia.systemKey)
        assertEquals(CategoryLevel.GENERAL, migratedMoradia.level)
        assertTrue(migratedMoradia.isDefault)

        val migratedAgua = categoryRepository.findById(legacyAgua.id)!!
        assertEquals("MORADIA_AGUA", migratedAgua.systemKey)
        assertEquals(CategoryLevel.SPECIFIC, migratedAgua.level)
        assertEquals(legacyMoradia.id, migratedAgua.parentId)

        val outrosGeneral = userCats.first { it.systemKey == "OUTROS" }
        val migratedPet = categoryRepository.findById(legacyPet.id)!!
        assertEquals(CategoryLevel.SPECIFIC, migratedPet.level)
        assertEquals(outrosGeneral.id, migratedPet.parentId)

        // Direct transaction on Moradia was moved to "Outros (Moradia)"
        val updatedTxMoradia = transactionRepository.findById(txOnMoradia.id)!!
        assertTrue(updatedTxMoradia.categoryId != legacyMoradia.id)
        val fallbackCategory = categoryRepository.findById(updatedTxMoradia.categoryId!!)!!
        assertEquals("Outros (Moradia)", fallbackCategory.name)
        assertEquals(CategoryLevel.SPECIFIC, fallbackCategory.level)
        assertEquals(legacyMoradia.id, fallbackCategory.parentId)

        // Transactions on Agua & Pet preserved their categoryId
        assertEquals(legacyAgua.id, transactionRepository.findById(txOnAgua.id)!!.categoryId)
        assertEquals(legacyPet.id, transactionRepository.findById(txOnPet.id)!!.categoryId)

        // Group with legacy got "Outros" and "Churrasco" nested under it
        val groupCats = categoryRepository.findByGroupId(groupWithLegacy.id)
        val groupOutros = groupCats.firstOrNull { it.systemKey == "OUTROS" }
        assertNotNull(groupOutros)
        val migratedGroupChurrasco = categoryRepository.findById(legacyGroupCat.id)!!
        assertEquals(CategoryLevel.SPECIFIC, migratedGroupChurrasco.level)
        assertEquals(groupOutros.id, migratedGroupChurrasco.parentId)

        // Empty group did NOT get unnecessary "Outros"
        assertEquals(0, categoryRepository.findByGroupId(emptyGroup.id).size)

        // Run migration 2nd time -> must be 100% idempotent
        val countBeforeSecondRun = userCats.size
        categorySeedService.migrateLegacyCategoriesAndTransactions()
        assertEquals(countBeforeSecondRun, categoryRepository.findByUserId(user.id).size)
        assertEquals(2, categoryRepository.findByGroupId(groupWithLegacy.id).size)

        // Delete a SPECIFIC category and verify transaction.categoryId is cleared to null
        deleteCategoryUseCase.execute(user.id, legacyPet.id)
        assertNull(categoryRepository.findById(legacyPet.id))
        assertNull(transactionRepository.findById(txOnPet.id)!!.categoryId)

        // Delete a default category and restore missing defaults via SeedDefaultCategoriesUseCase
        deleteCategoryUseCase.execute(user.id, legacyAgua.id)
        assertNull(categoryRepository.findById(legacyAgua.id))

        val restored = seedDefaultCategoriesUseCase.seedForUser(user.id)
        assertTrue(restored.any { it.name == "Água" && it.level == CategoryLevel.SPECIFIC && it.parentId == legacyMoradia.id })

        // On-demand group seeding
        val groupSeeded = seedDefaultCategoriesUseCase.seedForGroup(emptyGroup.id, user.id)
        assertEquals(
            DefaultCategoryCatalog.generals.size + DefaultCategoryCatalog.generals.sumOf { it.specifics.size },
            groupSeeded.size
        )

        // List user categories with level filter
        val generalsOnly = listUserCategoriesUseCase.execute(user.id, level = CategoryLevel.GENERAL)
        assertEquals(DefaultCategoryCatalog.generals.size, generalsOnly.size)
    }

    @Test
    fun `should preserve custom GENERAL without children and not recreate intentionally deleted defaults on boot`() {
        val user = userRepository.save(
            UserEntity(
                firstName = Name("Migrated"),
                lastName = Name("User"),
                email = Email("migrated.${UUID.randomUUID()}@finance.com"),
                passwordHash = PasswordHash("hash")
            )
        )

        categorySeedService.seedDefaultsForUser(user.id)

        // User creates a new custom GENERAL category without children (hierarchyMigrated = true by default)
        val customGeneral = categoryRepository.save(
            CategoryEntity(
                name = "Projetos Pessoais",
                description = null,
                userId = user.id,
                groupId = null,
                type = CategoryType.EXPENSE,
                color = "#7D39EB",
                icon = "🚀",
                level = CategoryLevel.GENERAL
            )
        )

        // User intentionally deletes a default SPECIFIC category ("IPTU")
        val iptu = categoryRepository.findByUserId(user.id).first { it.systemKey == "MORADIA_IPTU" }
        deleteCategoryUseCase.execute(user.id, iptu.id)
        assertNull(categoryRepository.findById(iptu.id))

        // Simulate application boot migration
        categorySeedService.migrateLegacyCategoriesAndTransactions()

        // Custom GENERAL without children MUST stay GENERAL (not nested under Outros)
        val persistedCustom = categoryRepository.findById(customGeneral.id)
        assertNotNull(persistedCustom)
        assertEquals(CategoryLevel.GENERAL, persistedCustom.level)
        assertNull(persistedCustom.parentId)

        // Intentionally deleted default MUST NOT be recreated on boot
        assertNull(categoryRepository.findByUserId(user.id).firstOrNull { it.systemKey == "MORADIA_IPTU" })

        // Explicit on-demand restore DOES recreate missing defaults
        val restoredList = seedDefaultCategoriesUseCase.seedForUser(user.id)
        assertTrue(restoredList.any { it.name == "IPTU" && it.level == CategoryLevel.SPECIFIC })
    }
}
