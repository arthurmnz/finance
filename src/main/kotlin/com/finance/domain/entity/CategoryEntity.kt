package com.finance.domain.entity

import com.finance.domain.enum.CategoryType
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
    val createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now()
) {
    var name: String = name
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

    var updatedAt: LocalDateTime = updatedAt
        private set

    init {
        require(userId != null || groupId != null) { "Category must belong to a user or a group" }
    }

    constructor(
        name: String,
        description: String?,
        userId: UUID?,
        groupId: UUID?,
        type: CategoryType,
        color: String?,
        icon: String?
    ) : this(
        id = UUID.randomUUID(),
        name = name,
        description = description,
        userId = userId,
        groupId = groupId,
        type = type,
        color = color,
        icon = icon,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    fun update(
        newName: String,
        newDescription: String?,
        newType: CategoryType,
        newColor: String?,
        newIcon: String?
    ) {
        this.name = newName
        this.description = newDescription
        this.type = newType
        this.color = newColor
        this.icon = newIcon
        this.updatedAt = LocalDateTime.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CategoryEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
