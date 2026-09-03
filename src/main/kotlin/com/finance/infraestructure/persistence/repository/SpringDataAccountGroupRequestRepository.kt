package com.finance.infraestructure.persistence.repository

import com.finance.domain.enum.AccountGroupRequestStatus
import com.finance.infraestructure.persistence.entity.AccountGroupRequestJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SpringDataAccountGroupRequestRepository : JpaRepository<AccountGroupRequestJpaEntity, UUID> {
    fun findByAccountIdAndStatus(accountId: UUID, status: AccountGroupRequestStatus): AccountGroupRequestJpaEntity?
    fun findAllByGroupIdAndStatus(groupId: UUID, status: AccountGroupRequestStatus): List<AccountGroupRequestJpaEntity>
    fun deleteAllByGroupId(groupId: UUID)
    fun deleteAllByAccountId(accountId: UUID)
}
