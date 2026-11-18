package com.finance.domain.entity

import com.finance.domain.enum.CategoryLevel
import com.finance.domain.enum.CategoryType
import com.finance.domain.exception.InvalidCategoryException
import java.time.LocalDateTime
import java.util.UUID

class CategoryEntity(
    val id: UUID,
    name: String,
    description: String?,
    userId: UUID?,
    groupId: UUID?,
    type: CategoryType,
    color: String?,
    icon: String?,
    level: CategoryLevel = CategoryLevel.GENERAL,
    parentId: UUID? = null,
    systemKey: String? = null,
    isDefault: Boolean = false,
    hierarchyMigrated: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now()
) {
    var name: String = name.trim()
        private set

    var description: String? = description
        private set

    var userId: UUID? = userId
        private set

    var groupId: UUID? = groupId
        private set

    var type: CategoryType = type
        private set

    var color: String? = color
        private set

    var icon: String? = icon
        private set

    var level: CategoryLevel = level
        private set

    var parentId: UUID? = parentId
        private set

    var systemKey: String? = systemKey
        private set

    var isDefault: Boolean = isDefault
        private set

    var hierarchyMigrated: Boolean = hierarchyMigrated
        private set

    var updatedAt: LocalDateTime = updatedAt
        private set

    init {
        require(userId != null || groupId != null) { "Category must belong to a user or a group" }
        validateCategory(
            name = name,
            description = description,
            color = color,
            icon = icon,
            level = level,
            parentId = parentId,
            selfId = id
        )
    }

    constructor(
        name: String,
        description: String?,
        userId: UUID?,
        groupId: UUID?,
        type: CategoryType,
        color: String?,
        icon: String?,
        level: CategoryLevel = CategoryLevel.GENERAL,
        parentId: UUID? = null,
        systemKey: String? = null,
        isDefault: Boolean = false,
        hierarchyMigrated: Boolean = true
    ) : this(
        id = UUID.randomUUID(),
        name = name,
        description = description,
        userId = userId,
        groupId = groupId,
        type = type,
        color = color,
        icon = icon,
        level = level,
        parentId = parentId,
        systemKey = systemKey,
        isDefault = isDefault,
        hierarchyMigrated = hierarchyMigrated,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    fun update(
        newName: String,
        newDescription: String?,
        newType: CategoryType,
        newColor: String?,
        newIcon: String?,
        newLevel: CategoryLevel = this.level,
        newParentId: UUID? = this.parentId
    ) {
        validateCategory(
            name = newName,
            description = newDescription,
            color = newColor,
            icon = newIcon,
            level = newLevel,
            parentId = newParentId,
            selfId = this.id
        )
        this.name = newName.trim()
        this.description = newDescription
        this.type = newType
        this.color = newColor
        this.icon = newIcon
        this.level = newLevel
        this.parentId = newParentId
        this.updatedAt = LocalDateTime.now()
    }

    fun promoteToDefaultGeneral(
        systemKey: String,
        newType: CategoryType = this.type,
        markMigrated: Boolean = true
    ) {
        validateCategory(
            name = this.name,
            description = this.description,
            color = this.color,
            icon = this.icon,
            level = CategoryLevel.GENERAL,
            parentId = null,
            selfId = this.id
        )
        this.level = CategoryLevel.GENERAL
        this.parentId = null
        this.type = newType
        this.systemKey = systemKey
        this.isDefault = true
        if (markMigrated) {
            this.hierarchyMigrated = true
        }
        this.updatedAt = LocalDateTime.now()
    }

    fun attachToParent(
        parentId: UUID,
        systemKey: String? = this.systemKey,
        isDefault: Boolean = this.isDefault
    ) {
        validateCategory(
            name = this.name,
            description = this.description,
            color = this.color,
            icon = this.icon,
            level = CategoryLevel.SPECIFIC,
            parentId = parentId,
            selfId = this.id
        )
        this.level = CategoryLevel.SPECIFIC
        this.parentId = parentId
        this.systemKey = systemKey
        this.isDefault = isDefault
        this.hierarchyMigrated = true
        this.updatedAt = LocalDateTime.now()
    }

    fun markHierarchyMigrated() {
        this.hierarchyMigrated = true
        this.updatedAt = LocalDateTime.now()
    }

    private fun validateCategory(
        name: String,
        description: String?,
        color: String?,
        icon: String?,
        level: CategoryLevel,
        parentId: UUID?,
        selfId: UUID
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            throw InvalidCategoryException("Nome da categoria não pode ser vazio.")
        }
        if (trimmedName.length > MAX_NAME_LENGTH) {
            throw InvalidCategoryException("Nome da categoria deve ter no máximo $MAX_NAME_LENGTH caracteres.")
        }
        if (description != null && description.length > MAX_DESCRIPTION_LENGTH) {
            throw InvalidCategoryException("Descrição da categoria deve ter no máximo $MAX_DESCRIPTION_LENGTH caracteres.")
        }
        if (color != null && color.length > MAX_COLOR_LENGTH) {
            throw InvalidCategoryException("Cor da categoria deve ter no máximo $MAX_COLOR_LENGTH caracteres.")
        }
        if (icon != null && icon.length > MAX_ICON_LENGTH) {
            throw InvalidCategoryException("Ícone da categoria deve ter no máximo $MAX_ICON_LENGTH caracteres.")
        }
        if (level == CategoryLevel.GENERAL && parentId != null) {
            throw InvalidCategoryException("Categorias gerais não podem possuir categoria pai.")
        }
        if (level == CategoryLevel.SPECIFIC && parentId == null) {
            throw InvalidCategoryException("Categorias específicas devem pertencer a uma categoria geral.")
        }
        if (parentId != null && parentId == selfId) {
            throw InvalidCategoryException("Uma categoria não pode ser pai de si mesma.")
        }
    }

    companion object {
        const val MAX_NAME_LENGTH = 100
        const val MAX_DESCRIPTION_LENGTH = 255
        const val MAX_COLOR_LENGTH = 20
        const val MAX_ICON_LENGTH = 50
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CategoryEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
