package com.finance.infraestructure.persistence.repository

import com.finance.domain.entity.TransactionEntity
import com.finance.domain.repository.TransactionRepository
import com.finance.infraestructure.persistence.mapper.TransactionMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
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

    override fun findByCategoryIdIn(categoryIds: Collection<UUID>): List<TransactionEntity> {
        if (categoryIds.isEmpty()) return emptyList()
        return springDataTransactionRepository.findByCategoryIdIn(categoryIds)
            .map { mapper.toDomain(it) }
    }

    override fun findByCategoryIdInOrGeneralCategoryId(
        categoryIds: Collection<UUID>,
        generalCategoryId: UUID
    ): List<TransactionEntity> {
        if (categoryIds.isEmpty()) return findByGeneralCategoryId(generalCategoryId)
        return springDataTransactionRepository
            .findByCategoryIdInOrGeneralCategoryId(categoryIds, generalCategoryId)
            .map { mapper.toDomain(it) }
    }

    override fun findByGeneralCategoryId(generalCategoryId: UUID): List<TransactionEntity> {
        return springDataTransactionRepository.findByGeneralCategoryId(generalCategoryId)
            .map { mapper.toDomain(it) }
    }

    override fun findByCategoryIdIsNotNullAndGeneralCategoryIdIsNull(): List<TransactionEntity> {
        return springDataTransactionRepository.findByCategoryIdIsNotNullAndGeneralCategoryIdIsNull()
            .map { mapper.toDomain(it) }
    }

    @Transactional
    override fun clearCategoryId(categoryId: UUID) {
        springDataTransactionRepository.clearCategoryId(categoryId)
    }

    @Transactional
    override fun clearCategoryReferences(categoryId: UUID) {
        springDataTransactionRepository.clearCategoryReferences(categoryId)
    }

    @Transactional
    override fun fallbackSpecificCategoryToGeneral(specificCategoryId: UUID, generalCategoryId: UUID) {
        springDataTransactionRepository.fallbackSpecificCategoryToGeneral(specificCategoryId, generalCategoryId)
    }

    @Transactional
    override fun updateGeneralCategoryForCategory(categoryId: UUID, newGeneralCategoryId: UUID) {
        springDataTransactionRepository.updateGeneralCategoryForCategory(categoryId, newGeneralCategoryId)
    }

    @Transactional
    override fun reassignCategory(fromCategoryId: UUID, toCategoryId: UUID) {
        springDataTransactionRepository.reassignCategory(fromCategoryId, toCategoryId)
    }

    override fun findByDestinationAccountId(destinationAccountId: UUID): List<TransactionEntity> {
        return springDataTransactionRepository.findByDestinationAccountId(destinationAccountId)
            .map { mapper.toDomain(it) }
    }

    override fun findByCreditCardId(creditCardId: UUID): List<TransactionEntity> {
        return springDataTransactionRepository.findByCreditCardId(creditCardId)
            .map { mapper.toDomain(it) }
    }

    override fun findByCreditCardInvoiceId(creditCardInvoiceId: UUID): List<TransactionEntity> {
        return springDataTransactionRepository.findByCreditCardInvoiceId(creditCardInvoiceId)
            .map { mapper.toDomain(it) }
    }

    override fun findByResponsibleId(responsibleId: UUID): List<TransactionEntity> {
        return springDataTransactionRepository.findByResponsibleId(responsibleId)
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

    override fun findFutureTransactionsByGroupId(groupId: UUID, date: LocalDate): List<TransactionEntity> {
        return springDataTransactionRepository.findFutureTransactionsByGroupId(groupId, date)
            .map { mapper.toDomain(it) }
    }

    override fun findByAccountIdInAndDateBetween(
        accountIds: List<UUID>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<TransactionEntity> {
        return springDataTransactionRepository.findByAccountIdInAndDateBetween(accountIds, startDate, endDate)
            .map { mapper.toDomain(it) }
    }

    override fun findByResponsibleIdAndDateBetween(
        responsibleId: UUID,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<TransactionEntity> {
        return springDataTransactionRepository.findByResponsibleIdAndDateBetween(responsibleId, startDate, endDate)
            .map { mapper.toDomain(it) }
    }
}
