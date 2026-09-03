package com.finance.infraestructure.persistence.repository

import com.finance.domain.entity.AccountGroupRequestEntity
import com.finance.domain.enum.AccountGroupRequestStatus
import com.finance.domain.repository.AccountGroupRequestRepository
import com.finance.infraestructure.persistence.mapper.AccountGroupRequestMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
class AccountGroupRequestRepositoryImpl(
    private val springDataAccountGroupRequestRepository: SpringDataAccountGroupRequestRepository,
    private val accountGroupRequestMapper: AccountGroupRequestMapper
) : AccountGroupRequestRepository {

    @Transactional
    override fun save(request: AccountGroupRequestEntity): AccountGroupRequestEntity {
        val saved = springDataAccountGroupRequestRepository.save(accountGroupRequestMapper.toJpaEntity(request))
        return accountGroupRequestMapper.toDomain(saved)
    }

    @Transactional(readOnly = true)
    override fun findById(id: UUID): AccountGroupRequestEntity? {
        return springDataAccountGroupRequestRepository.findById(id).map { accountGroupRequestMapper.toDomain(it) }.orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByAccountIdAndStatus(accountId: UUID, status: AccountGroupRequestStatus): AccountGroupRequestEntity? {
        return springDataAccountGroupRequestRepository.findByAccountIdAndStatus(accountId, status)?.let { accountGroupRequestMapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findAllByGroupIdAndStatus(groupId: UUID, status: AccountGroupRequestStatus): List<AccountGroupRequestEntity> {
        return springDataAccountGroupRequestRepository.findAllByGroupIdAndStatus(groupId, status).map { accountGroupRequestMapper.toDomain(it) }
    }

    @Transactional
    override fun deleteAllByGroupId(groupId: UUID) {
        springDataAccountGroupRequestRepository.deleteAllByGroupId(groupId)
    }

    @Transactional
    override fun deleteAllByAccountId(accountId: UUID) {
        springDataAccountGroupRequestRepository.deleteAllByAccountId(accountId)
    }
}
