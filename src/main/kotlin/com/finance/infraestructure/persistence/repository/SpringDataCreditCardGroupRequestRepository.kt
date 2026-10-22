package com.finance.infraestructure.persistence.repository

import com.finance.domain.enum.AccountGroupRequestStatus
import com.finance.infraestructure.persistence.entity.CreditCardGroupRequestJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SpringDataCreditCardGroupRequestRepository : JpaRepository<CreditCardGroupRequestJpaEntity, UUID> {
    fun findByCreditCardIdAndStatus(creditCardId: UUID, status: AccountGroupRequestStatus): CreditCardGroupRequestJpaEntity?
    fun findAllByGroupIdAndStatus(groupId: UUID, status: AccountGroupRequestStatus): List<CreditCardGroupRequestJpaEntity>
    fun deleteAllByCreditCardId(creditCardId: UUID)
    fun deleteAllByGroupId(groupId: UUID)
}
