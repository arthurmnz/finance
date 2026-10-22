package com.finance.domain.repository

import com.finance.domain.entity.CreditCardGroupRequestEntity
import com.finance.domain.enum.AccountGroupRequestStatus
import java.util.UUID

interface CreditCardGroupRequestRepository {
    fun save(request: CreditCardGroupRequestEntity): CreditCardGroupRequestEntity
    fun findById(id: UUID): CreditCardGroupRequestEntity?
    fun findByCreditCardIdAndStatus(creditCardId: UUID, status: AccountGroupRequestStatus): CreditCardGroupRequestEntity?
    fun findAllByGroupIdAndStatus(groupId: UUID, status: AccountGroupRequestStatus): List<CreditCardGroupRequestEntity>
    fun deleteAllByCreditCardId(creditCardId: UUID)
    fun deleteAllByGroupId(groupId: UUID)
}
