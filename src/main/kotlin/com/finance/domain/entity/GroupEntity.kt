package com.finance.domain.entity

import java.time.LocalDateTime
import java.util.UUID

class GroupEntity(
    val id: UUID,
    name: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    var name: String = name
        private set

    var updatedAt: LocalDateTime = updatedAt
        private set

    constructor(name: String) : this(
        id = UUID.randomUUID(),
        name = name.trim(),
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
    ) {
        require(name.isNotBlank()) { "Nome do grupo não pode ser vazio." }
        require(name.trim().length <= 100) { "Nome do grupo não pode ter mais de 100 caracteres." }
    }

    fun rename(newName: String) {
        require(newName.isNotBlank()) { "Nome do grupo não pode ser vazio." }
        require(newName.trim().length <= 100) { "Nome do grupo não pode ter mais de 100 caracteres." }
        this.name = newName.trim()
        this.updatedAt = LocalDateTime.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String =
        "GroupEntity(id=$id, name=$name, createdAt=$createdAt, updatedAt=$updatedAt)"
}
