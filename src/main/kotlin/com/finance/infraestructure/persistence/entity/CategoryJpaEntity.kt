package com.finance.infraestructure.persistence.entity

import com.finance.domain.enum.CategoryType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "categories")
class CategoryJpaEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,

    @Column(name = "name", nullable = false, length = 100)
    var name: String,

    @Column(name = "description")
    var description: String?,

    @Column(name = "user_id")
    var userId: UUID?,

    @Column(name = "group_id")
    var groupId: UUID?,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    var type: CategoryType,

    @Column(name = "color", length = 20)
    var color: String?,

    @Column(name = "icon", length = 50)
    var icon: String?,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
