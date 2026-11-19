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
    fun `should migrate legacy user and group categories idempotently preserving direct general transactions and backfilling generalCategoryId`() {
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
        assertTrue(userCats.none { it.name == "Outros (Moradia)" })

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

        // Direct transaction on Moradia is PRESERVED directly on Moradia and backfilled with generalCategoryId
        val updatedTxMoradia = transactionRepository.findById(txOnMoradia.id)!!
        assertEquals(legacyMoradia.id, updatedTxMoradia.categoryId)
        assertEquals(legacyMoradia.id, updatedTxMoradia.generalCategoryId)

        // Transactions on Agua & Pet preserved their categoryId and had generalCategoryId backfilled
        val updatedTxAgua = transactionRepository.findById(txOnAgua.id)!!
        assertEquals(legacyAgua.id, updatedTxAgua.categoryId)
        assertEquals(legacyMoradia.id, updatedTxAgua.generalCategoryId)

        val updatedTxPet = transactionRepository.findById(txOnPet.id)!!
        assertEquals(legacyPet.id, updatedTxPet.categoryId)
        assertEquals(outrosGeneral.id, updatedTxPet.generalCategoryId)

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

        // Delete a SPECIFIC category and verify transaction falls back to parent GENERAL
        deleteCategoryUseCase.execute(user.id, legacyPet.id)
        assertNull(categoryRepository.findById(legacyPet.id))
        val fallbackPetTx = transactionRepository.findById(txOnPet.id)!!
        assertEquals(outrosGeneral.id, fallbackPetTx.categoryId)
        assertEquals(outrosGeneral.id, fallbackPetTx.generalCategoryId)

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
    fun `should backfill generalCategoryId for pre-existing transactions and clear references when deleting leaf GENERAL`() {
        val user = userRepository.save(
            UserEntity(
                firstName = Name("Backfill"),
                lastName = Name("User"),
                email = Email("backfill.${UUID.randomUUID()}@finance.com"),
                passwordHash = PasswordHash("hash")
            )
        )
        val account = accountRepository.save(
            AccountEntity(
                ownerId = user.id,
                name = "Conta",
                startBalance = 50000,
                type = AccountType.CHECKING,
                tax = null
            )
        )
        categorySeedService.seedDefaultsForUser(user.id)

        val moradia = categoryRepository.findByUserId(user.id).first { it.systemKey == "MORADIA" }
        val aluguel = categoryRepository.findByUserId(user.id).first { it.systemKey == "MORADIA_ALUGUEL" }
        val customLeafGeneral = categoryRepository.save(
            CategoryEntity(
                name = "Viagens Especiais",
                description = null,
                userId = user.id,
                groupId = null,
                type = CategoryType.EXPENSE,
                color = "#123456",
                icon = "✈️",
                level = CategoryLevel.GENERAL
            )
        )

        val txGeneralOnlyNullBackfill = transactionRepository.save(
            TransactionEntity(
                title = "Manutenção Geral",
                amount = 10000,
                date = LocalDate.of(2026, 6, 1),
                responsibleId = user.id,
                accountId = account.id,
                type = TransactionType.EXPENSE,
                destinationAccountId = null,
                categoryId = moradia.id,
                status = TransactionStatus.COMPLETED,
                generalCategoryId = null
            )
        )
        val txSpecificNullBackfill = transactionRepository.save(
            TransactionEntity(
                title = "Aluguel Junho",
                amount = 200000,
                date = LocalDate.of(2026, 6, 2),
                responsibleId = user.id,
                accountId = account.id,
                type = TransactionType.EXPENSE,
                destinationAccountId = null,
                categoryId = aluguel.id,
                status = TransactionStatus.COMPLETED,
                generalCategoryId = null
            )
        )
        val txOnLeafGeneral = transactionRepository.save(
            TransactionEntity(
                title = "Passagem",
                amount = 80000,
                date = LocalDate.of(2026, 6, 3),
                responsibleId = user.id,
                accountId = account.id,
                type = TransactionType.EXPENSE,
                destinationAccountId = null,
                categoryId = customLeafGeneral.id,
                status = TransactionStatus.COMPLETED,
                generalCategoryId = customLeafGeneral.id
            )
        )

        categorySeedService.backfillTransactionGeneralCategories()

        val reloadedGeneralTx = transactionRepository.findById(txGeneralOnlyNullBackfill.id)!!
        assertEquals(moradia.id, reloadedGeneralTx.categoryId)
        assertEquals(moradia.id, reloadedGeneralTx.generalCategoryId)

        val reloadedSpecificTx = transactionRepository.findById(txSpecificNullBackfill.id)!!
        assertEquals(aluguel.id, reloadedSpecificTx.categoryId)
        assertEquals(moradia.id, reloadedSpecificTx.generalCategoryId)

        // Deleting leaf GENERAL clears both categoryId and generalCategoryId
        deleteCategoryUseCase.execute(user.id, customLeafGeneral.id)
        val reloadedLeafTx = transactionRepository.findById(txOnLeafGeneral.id)!!
        assertNull(reloadedLeafTx.categoryId)
        assertNull(reloadedLeafTx.generalCategoryId)
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
