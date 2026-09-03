package com.finance.domain.entity

import com.finance.domain.enum.AccountGroupRequestStatus
import java.time.LocalDateTime
import java.util.UUID

class AccountGroupRequestEntity(
    val id: UUID,
    val accountId: UUID,
    val groupId: UUID,
    val requesterId: UUID,
    status: AccountGroupRequestStatus,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now()
) {
    var status: AccountGroupRequestStatus = status
        private set

    var updatedAt: LocalDateTime = updatedAt
        private set

    constructor(accountId: UUID, groupId: UUID, requesterId: UUID) : this(
        id = UUID.randomUUID(),
        accountId = accountId,
        groupId = groupId,
        requesterId = requesterId,
        status = AccountGroupRequestStatus.PENDING
    )

    fun accept() {
        check(status == AccountGroupRequestStatus.PENDING) { "Apenas solicitações pendentes podem ser aceitas." }
        this.status = AccountGroupRequestStatus.ACCEPTED
        this.updatedAt = LocalDateTime.now()
    }

    fun reject() {
        check(status == AccountGroupRequestStatus.PENDING) { "Apenas solicitações pendentes podem ser recusadas." }
        this.status = AccountGroupRequestStatus.REJECTED
        this.updatedAt = LocalDateTime.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AccountGroupRequestEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
