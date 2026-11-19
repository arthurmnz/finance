package com.finance.application.service

import com.finance.domain.entity.CategoryEntity
import com.finance.domain.enum.CategoryLevel
import com.finance.domain.enum.CategoryType
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CategorySeedService(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun seedDefaultsForUser(userId: UUID) {
        val existing = categoryRepository.findByUserId(userId)
        seedDefaultsForScope(userId = userId, groupId = null, existingCategories = existing)
    }

    @Transactional
    fun seedDefaultsForGroup(groupId: UUID) {
        val existing = categoryRepository.findByGroupId(groupId)
        seedDefaultsForScope(userId = null, groupId = groupId, existingCategories = existing)
    }

    @Transactional
    fun migrateLegacyCategoriesAndTransactions() {
        val users = userRepository.findAll()
        for (user in users) {
            migrateUserCategories(user.id)
        }

        val unmigratedGroupCategories = categoryRepository.findByHierarchyMigratedFalse()
            .filter { it.groupId != null }

        if (unmigratedGroupCategories.isNotEmpty()) {
            val byGroup = unmigratedGroupCategories.groupBy { it.groupId!! }
            for ((groupId, legacyGroupCats) in byGroup) {
                migrateGroupCategories(groupId, legacyGroupCats)
            }
        }

        backfillTransactionGeneralCategories()
    }

    private fun migrateUserCategories(userId: UUID) {
        val initialCategories = categoryRepository.findByUserId(userId)
        val hadUnmigrated = initialCategories.any { !it.hierarchyMigrated }

        if (initialCategories.isNotEmpty() && !hadUnmigrated) return

        seedDefaultsForScope(userId = userId, groupId = null, existingCategories = initialCategories)

        if (!hadUnmigrated) return

        val allUserCategories = categoryRepository.findByUserId(userId).toMutableList()
        val outrosGeneral = allUserCategories.firstOrNull {
            it.systemKey == DefaultCategoryCatalog.OUTROS_SYSTEM_KEY
        } ?: return

        val parentIdsWithChildren = allUserCategories.mapNotNull { it.parentId }.toSet()
        val toSave = mutableListOf<CategoryEntity>()

        for (cat in allUserCategories.filter { !it.hierarchyMigrated }) {
            if (cat.id in parentIdsWithChildren) {
                cat.markHierarchyMigrated()
                toSave.add(cat)
            } else if (cat.level == CategoryLevel.GENERAL && cat.parentId == null) {
                cat.attachToParent(parentId = outrosGeneral.id)
                toSave.add(cat)
            } else {
                cat.markHierarchyMigrated()
                toSave.add(cat)
            }
        }

        if (toSave.isNotEmpty()) {
            categoryRepository.saveAll(toSave)
        }
    }

    private fun migrateGroupCategories(groupId: UUID, legacyGroupCats: List<CategoryEntity>) {
        val allGroupCategories = categoryRepository.findByGroupId(groupId).toMutableList()
        val parentIdsWithChildren = allGroupCategories.mapNotNull { it.parentId }.toSet()

        val legacyToShelter = legacyGroupCats.filter {
            it.level == CategoryLevel.GENERAL && it.parentId == null && it.id !in parentIdsWithChildren
        }

        var outrosGeneral = allGroupCategories.firstOrNull {
            it.systemKey == DefaultCategoryCatalog.OUTROS_SYSTEM_KEY
        }

        if (legacyToShelter.isNotEmpty() && outrosGeneral == null) {
            val matchingOutros = allGroupCategories.firstOrNull {
                it.level == CategoryLevel.GENERAL &&
                    it.parentId == null &&
                    DefaultCategoryCatalog.normalizeName(it.name) == DefaultCategoryCatalog.normalizeName(DefaultCategoryCatalog.outrosSpec.name)
            }

            if (matchingOutros != null) {
                matchingOutros.promoteToDefaultGeneral(
                    systemKey = DefaultCategoryCatalog.OUTROS_SYSTEM_KEY,
                    newType = CategoryType.BOTH
                )
                outrosGeneral = categoryRepository.save(matchingOutros)
            } else {
                val spec = DefaultCategoryCatalog.outrosSpec
                val createdOutros = CategoryEntity(
                    name = spec.name,
                    description = spec.description,
                    userId = null,
                    groupId = groupId,
                    type = CategoryType.BOTH,
                    color = spec.color,
                    icon = spec.icon,
                    level = CategoryLevel.GENERAL,
                    parentId = null,
                    systemKey = spec.systemKey,
                    isDefault = true,
                    hierarchyMigrated = true
                )
                outrosGeneral = categoryRepository.save(createdOutros)
                allGroupCategories.add(outrosGeneral)
            }
        }

        val toSave = mutableListOf<CategoryEntity>()
        for (cat in legacyGroupCats) {
            if (outrosGeneral != null && cat.id == outrosGeneral.id) {
                if (!cat.hierarchyMigrated) {
                    cat.markHierarchyMigrated()
                    toSave.add(cat)
                }
                continue
            }

            if (cat.id in parentIdsWithChildren) {
                cat.markHierarchyMigrated()
                toSave.add(cat)
            } else if (cat.level == CategoryLevel.GENERAL && cat.parentId == null && outrosGeneral != null) {
                cat.attachToParent(parentId = outrosGeneral.id)
                toSave.add(cat)
            } else if (!cat.hierarchyMigrated) {
                cat.markHierarchyMigrated()
                toSave.add(cat)
            }
        }

        if (toSave.isNotEmpty()) {
            categoryRepository.saveAll(toSave)
        }
    }

    @Transactional
    fun backfillTransactionGeneralCategories() {
        val pendingTransactions = transactionRepository.findByCategoryIdIsNotNullAndGeneralCategoryIdIsNull()
        if (pendingTransactions.isEmpty()) return

        val categoryIds = pendingTransactions.mapNotNull { it.categoryId }.distinct()
        val categoriesById = categoryRepository.findByIdIn(categoryIds).associateBy { it.id }

        val updated = pendingTransactions.mapNotNull { tx ->
            val cat = tx.categoryId?.let { categoriesById[it] } ?: return@mapNotNull null
            val resolvedGeneralId = if (cat.level == CategoryLevel.GENERAL) {
                cat.id
            } else {
                cat.parentId ?: cat.id
            }
            tx.update(
                newTitle = tx.title,
                newAmount = tx.amount,
                newDate = tx.date,
                newCategoryId = tx.categoryId,
                newStatus = tx.status,
                newInvoiceMonth = tx.invoiceMonth,
                newInvoiceYear = tx.invoiceYear,
                newGeneratesDebt = tx.generatesDebt,
                newGeneralCategoryId = resolvedGeneralId
            )
            tx
        }

        if (updated.isNotEmpty()) {
            transactionRepository.saveAll(updated)
            logger.info("Backfill de generalCategoryId concluído para {} transações.", updated.size)
        }
    }

    private fun seedDefaultsForScope(
        userId: UUID?,
        groupId: UUID?,
        existingCategories: List<CategoryEntity>
    ) {
        val workingList = existingCategories.toMutableList()
        val bySystemKey = workingList
            .filter { it.systemKey != null }
            .associateByTo(mutableMapOf()) { it.systemKey!! }
        val parentIdsWithChildren = workingList.mapNotNull { it.parentId }.toMutableSet()
        val toPersist = mutableListOf<CategoryEntity>()

        for (generalSpec in DefaultCategoryCatalog.generals) {
            val normalizedGeneralName = DefaultCategoryCatalog.normalizeName(generalSpec.name)

            val existingGeneral = bySystemKey[generalSpec.systemKey]
                ?: workingList.firstOrNull { cat ->
                    cat.systemKey == null &&
                        cat.level == CategoryLevel.GENERAL &&
                        cat.parentId == null &&
                        DefaultCategoryCatalog.normalizeName(cat.name) == normalizedGeneralName &&
                        (cat.type == generalSpec.type || generalSpec.type == CategoryType.BOTH)
                }?.also { matched ->
                    val targetType = if (generalSpec.type == CategoryType.BOTH) CategoryType.BOTH else matched.type
                    matched.promoteToDefaultGeneral(
                        systemKey = generalSpec.systemKey,
                        newType = targetType,
                        markMigrated = true
                    )
                    bySystemKey[generalSpec.systemKey] = matched
                    toPersist.add(matched)
                }

            val generalEntity = existingGeneral ?: CategoryEntity(
                name = generalSpec.name,
                description = generalSpec.description,
                userId = userId,
                groupId = groupId,
                type = generalSpec.type,
                color = generalSpec.color,
                icon = generalSpec.icon,
                level = CategoryLevel.GENERAL,
                parentId = null,
                systemKey = generalSpec.systemKey,
                isDefault = true,
                hierarchyMigrated = true
            ).also { created ->
                workingList.add(created)
                bySystemKey[generalSpec.systemKey] = created
                toPersist.add(created)
            }

            for (specificSpec in generalSpec.specifics) {
                val normalizedSpecificName = DefaultCategoryCatalog.normalizeName(specificSpec.name)

                val existingSpecific = bySystemKey[specificSpec.systemKey]
                    ?: workingList.firstOrNull { cat ->
                        cat.systemKey == null &&
                            cat.parentId == generalEntity.id &&
                            DefaultCategoryCatalog.normalizeName(cat.name) == normalizedSpecificName
                    }?.also { matched ->
                        matched.attachToParent(
                            parentId = generalEntity.id,
                            systemKey = specificSpec.systemKey,
                            isDefault = true
                        )
                        bySystemKey[specificSpec.systemKey] = matched
                        toPersist.add(matched)
                    }
                    ?: workingList.firstOrNull { cat ->
                        cat.systemKey == null &&
                            !cat.hierarchyMigrated &&
                            cat.id != generalEntity.id &&
                            cat.parentId == null &&
                            cat.id !in parentIdsWithChildren &&
                            DefaultCategoryCatalog.normalizeName(cat.name) == normalizedSpecificName &&
                            cat.type == specificSpec.type
                    }?.also { legacyMatched ->
                        legacyMatched.attachToParent(
                            parentId = generalEntity.id,
                            systemKey = specificSpec.systemKey,
                            isDefault = true
                        )
                        parentIdsWithChildren.add(generalEntity.id)
                        bySystemKey[specificSpec.systemKey] = legacyMatched
                        toPersist.add(legacyMatched)
                    }

                if (existingSpecific == null) {
                    val createdSpecific = CategoryEntity(
                        name = specificSpec.name,
                        description = specificSpec.description,
                        userId = userId,
                        groupId = groupId,
                        type = specificSpec.type,
                        color = specificSpec.color,
                        icon = specificSpec.icon,
                        level = CategoryLevel.SPECIFIC,
                        parentId = generalEntity.id,
                        systemKey = specificSpec.systemKey,
                        isDefault = true,
                        hierarchyMigrated = true
                    )
                    workingList.add(createdSpecific)
                    parentIdsWithChildren.add(generalEntity.id)
                    bySystemKey[specificSpec.systemKey] = createdSpecific
                    toPersist.add(createdSpecific)
                }
            }
        }

        if (toPersist.isNotEmpty()) {
            categoryRepository.saveAll(toPersist.distinctBy { it.id })
        }
    }
}
