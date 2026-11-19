package com.finance.domain.entity

import java.time.LocalDateTime
import java.util.UUID

class JointAccountMemberEntity(
    val id: UUID = UUID.randomUUID(),
    val accountId: UUID,
    val userId: UUID,
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JointAccountMemberEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
