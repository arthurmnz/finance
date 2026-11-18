package com.finance.domain.entity

import com.finance.domain.enum.CategoryLevel
import com.finance.domain.enum.CategoryType
import com.finance.domain.exception.InvalidCategoryException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CategoryEntityTest {

    @Test
    fun `should create valid GENERAL and SPECIFIC categories and trim name`() {
        val userId = UUID.randomUUID()
        val general = CategoryEntity(
            name = "  Moradia  ",
            description = null,
            userId = userId,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#7D39EB",
            icon = "🏠",
            level = CategoryLevel.GENERAL
        )
        assertEquals("Moradia", general.name)
        assertEquals(CategoryLevel.GENERAL, general.level)
        assertNull(general.parentId)

        val specific = CategoryEntity(
            name = "Aluguel",
            description = null,
            userId = userId,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = "#7D39EB",
            icon = "🔑",
            level = CategoryLevel.SPECIFIC,
            parentId = general.id
        )
        assertEquals(CategoryLevel.SPECIFIC, specific.level)
        assertEquals(general.id, specific.parentId)
    }

    @Test
    fun `should reject blank name, GENERAL with parentId, SPECIFIC without parentId, and self-parent`() {
        val userId = UUID.randomUUID()
        val id = UUID.randomUUID()

        assertThrows<InvalidCategoryException> {
            CategoryEntity(
                name = "   ",
                description = null,
                userId = userId,
                groupId = null,
                type = CategoryType.EXPENSE,
                color = null,
                icon = null
            )
        }

        assertThrows<InvalidCategoryException> {
            CategoryEntity(
                name = "Moradia",
                description = null,
                userId = userId,
                groupId = null,
                type = CategoryType.EXPENSE,
                color = null,
                icon = null,
                level = CategoryLevel.GENERAL,
                parentId = UUID.randomUUID()
            )
        }

        assertThrows<InvalidCategoryException> {
            CategoryEntity(
                name = "Aluguel",
                description = null,
                userId = userId,
                groupId = null,
                type = CategoryType.EXPENSE,
                color = null,
                icon = null,
                level = CategoryLevel.SPECIFIC,
                parentId = null
            )
        }

        assertThrows<InvalidCategoryException> {
            CategoryEntity(
                id = id,
                name = "AutoRef",
                description = null,
                userId = userId,
                groupId = null,
                type = CategoryType.EXPENSE,
                color = null,
                icon = null,
                level = CategoryLevel.SPECIFIC,
                parentId = id
            )
        }
    }

    @Test
    fun `should validate hierarchy invariants on update`() {
        val userId = UUID.randomUUID()
        val parentId = UUID.randomUUID()
        val category = CategoryEntity(
            name = "Aluguel",
            description = null,
            userId = userId,
            groupId = null,
            type = CategoryType.EXPENSE,
            color = null,
            icon = null,
            level = CategoryLevel.SPECIFIC,
            parentId = parentId
        )

        assertThrows<InvalidCategoryException> {
            category.update(
                newName = "",
                newDescription = null,
                newType = CategoryType.EXPENSE,
                newColor = null,
                newIcon = null
            )
        }

        assertThrows<InvalidCategoryException> {
            category.update(
                newName = "Aluguel",
                newDescription = null,
                newType = CategoryType.EXPENSE,
                newColor = null,
                newIcon = null,
                newLevel = CategoryLevel.SPECIFIC,
                newParentId = category.id
            )
        }

        assertThrows<InvalidCategoryException> {
            category.update(
                newName = "Aluguel",
                newDescription = null,
                newType = CategoryType.EXPENSE,
                newColor = null,
                newIcon = null,
                newLevel = CategoryLevel.GENERAL,
                newParentId = parentId
            )
        }
    }

    @Test
    fun `should reject fields exceeding JPA column lengths on init and update`() {
        val userId = UUID.randomUUID()
        val tooLongName = "a".repeat(101)
        val tooLongDesc = "d".repeat(256)
        val tooLongColor = "c".repeat(21)
        val tooLongIcon = "i".repeat(51)

        assertThrows<InvalidCategoryException> {
            CategoryEntity(tooLongName, null, userId, null, CategoryType.EXPENSE, null, null)
        }
        assertThrows<InvalidCategoryException> {
            CategoryEntity("Moradia", tooLongDesc, userId, null, CategoryType.EXPENSE, null, null)
        }
        assertThrows<InvalidCategoryException> {
            CategoryEntity("Moradia", null, userId, null, CategoryType.EXPENSE, tooLongColor, null)
        }
        assertThrows<InvalidCategoryException> {
            CategoryEntity("Moradia", null, userId, null, CategoryType.EXPENSE, null, tooLongIcon)
        }

        val valid = CategoryEntity("Moradia", null, userId, null, CategoryType.EXPENSE, "#7D39EB", "🏠")
        assertThrows<InvalidCategoryException> {
            valid.update(tooLongName, null, CategoryType.EXPENSE, null, null)
        }
        assertThrows<InvalidCategoryException> {
            valid.update("Moradia", tooLongDesc, CategoryType.EXPENSE, null, null)
        }
        assertThrows<InvalidCategoryException> {
            valid.update("Moradia", null, CategoryType.EXPENSE, tooLongColor, null)
        }
        assertThrows<InvalidCategoryException> {
            valid.update("Moradia", null, CategoryType.EXPENSE, null, tooLongIcon)
        }
    }
}
