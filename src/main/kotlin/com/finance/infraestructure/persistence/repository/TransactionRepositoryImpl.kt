package com.finance.infraestructure.persistence.repository

import com.finance.domain.entity.TransactionEntity
import com.finance.domain.repository.TransactionRepository
import com.finance.infraestructure.persistence.mapper.TransactionMapper
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class TransactionRepositoryImpl(
    private val springDataTransactionRepository: SpringDataTransactionRepository,
    private val mapper: TransactionMapper
) : TransactionRepository {

    override fun save(transaction: TransactionEntity): TransactionEntity {
        val jpaEntity = mapper.toJpaEntity(transaction)
        val savedEntity = springDataTransactionRepository.save(jpaEntity)
        return mapper.toDomain(savedEntity)
    }

    override fun findById(id: UUID): TransactionEntity? {
        return springDataTransactionRepository.findById(id)
            .map { mapper.toDomain(it) }
            .orElse(null)
    }

    override fun findByAccountId(accountId: UUID): List<TransactionEntity> {
        return springDataTransactionRepository.findByAccountId(accountId)
            .map { mapper.toDomain(it) }
    }

    override fun findByCategoryId(categoryId: UUID): List<TransactionEntity> {
        return springDataTransactionRepository.findByCategoryId(categoryId)
            .map { mapper.toDomain(it) }
    }

    override fun delete(id: UUID) {
        springDataTransactionRepository.deleteById(id)
    }

    override fun deleteAll(ids: List<UUID>) {
        springDataTransactionRepository.deleteAllById(ids)
    }

    override fun saveAll(transactions: List<TransactionEntity>): List<TransactionEntity> {
        val jpaEntities = transactions.map { mapper.toJpaEntity(it) }
        val savedEntities = springDataTransactionRepository.saveAll(jpaEntities)
        return savedEntities.map { mapper.toDomain(it) }
    }

    override fun findLatestInfiniteTransactions(): List<TransactionEntity> {
        return springDataTransactionRepository.findLatestInfiniteTransactions()
            .map { mapper.toDomain(it) }
    }

    override fun findFutureTransactionsByGroupId(groupId: UUID, date: java.time.LocalDateTime): List<TransactionEntity> {
        return springDataTransactionRepository.findFutureTransactionsByGroupId(groupId, date)
            .map { mapper.toDomain(it) }
    }
}
