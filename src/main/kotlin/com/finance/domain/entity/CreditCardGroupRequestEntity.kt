package com.finance.domain.entity

import com.finance.domain.enum.AccountGroupRequestStatus
import java.time.LocalDateTime
import java.util.UUID

class CreditCardGroupRequestEntity(
    val id: UUID = UUID.randomUUID(),
    val creditCardId: UUID,
    val groupId: UUID,
    val requesterId: UUID,
    status: AccountGroupRequestStatus = AccountGroupRequestStatus.PENDING,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now()
) {
    var status: AccountGroupRequestStatus = status
        private set

    var updatedAt: LocalDateTime = updatedAt
        private set

    fun accept() {
        check(status == AccountGroupRequestStatus.PENDING) { "Solicitação já foi processada." }
        status = AccountGroupRequestStatus.ACCEPTED
        updatedAt = LocalDateTime.now()
    }

    fun reject() {
        check(status == AccountGroupRequestStatus.PENDING) { "Solicitação já foi processada." }
        status = AccountGroupRequestStatus.REJECTED
        updatedAt = LocalDateTime.now()
    }
}
