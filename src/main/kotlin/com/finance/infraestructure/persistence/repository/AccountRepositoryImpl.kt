package com.finance.infraestructure.persistence.repository

import com.finance.domain.entity.AccountEntity
import com.finance.domain.repository.AccountRepository
import com.finance.infraestructure.persistence.mapper.AccountMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
class AccountRepositoryImpl(
    private val springDataAccountRepository: SpringDataAccountRepository,
    private val accountMapper: AccountMapper
) : AccountRepository {

    @Transactional
    override fun save(account: AccountEntity): AccountEntity {
        val saved = springDataAccountRepository.save(accountMapper.toJpaEntity(account))
        return accountMapper.toDomain(saved)
    }

    @Transactional(readOnly = true)
    override fun findById(id: UUID): AccountEntity? {
        return springDataAccountRepository.findById(id).map { accountMapper.toDomain(it) }.orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findAllByIdIn(ids: Collection<UUID>): List<AccountEntity> {
        if (ids.isEmpty()) return emptyList()
        return springDataAccountRepository.findAllByIdIn(ids).map { accountMapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findAllByOwnerId(ownerId: UUID): List<AccountEntity> {
        return springDataAccountRepository.findAllByOwnerId(ownerId).map { accountMapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findAllByGroupId(groupId: UUID): List<AccountEntity> {
        return springDataAccountRepository.findAllByGroupId(groupId).map { accountMapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findAllByGroupIdIn(groupIds: Collection<UUID>): List<AccountEntity> {
        if (groupIds.isEmpty()) return emptyList()
        return springDataAccountRepository.findAllByGroupIdIn(groupIds).map { accountMapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun findAllByGroupIdAndIsJointTrue(groupId: UUID): List<AccountEntity> {
        return springDataAccountRepository.findAllByGroupIdAndIsJointTrue(groupId).map { accountMapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun existsByOwnerIdAndGroupIdNotNull(ownerId: UUID): Boolean {
        return springDataAccountRepository.existsByOwnerIdAndGroupIdIsNotNull(ownerId)
    }

    @Transactional(readOnly = true)
    override fun existsByOwnerIdAndGroupId(ownerId: UUID, groupId: UUID): Boolean {
        return springDataAccountRepository.existsByOwnerIdAndGroupId(ownerId, groupId)
    }

    @Transactional
    override fun deleteAllByOwnerId(ownerId: UUID) {
        springDataAccountRepository.deleteAllByOwnerId(ownerId)
    }

    @Transactional
    override fun deleteAllByGroupIdAndIsJointTrue(groupId: UUID) {
        springDataAccountRepository.deleteAllByGroupIdAndIsJointTrue(groupId)
    }

    @Transactional
    override fun deleteById(id: UUID) {
        springDataAccountRepository.deleteById(id)
    }
}
