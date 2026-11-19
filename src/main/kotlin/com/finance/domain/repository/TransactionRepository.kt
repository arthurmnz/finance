package com.finance.domain.repository

import com.finance.domain.entity.TransactionEntity
import java.time.LocalDate
import java.util.UUID

interface TransactionRepository {
    fun save(transaction: TransactionEntity): TransactionEntity
    fun findById(id: UUID): TransactionEntity?
    fun findByAccountId(accountId: UUID): List<TransactionEntity>
    fun findByCategoryId(categoryId: UUID): List<TransactionEntity>
    fun findByCategoryIdIn(categoryIds: Collection<UUID>): List<TransactionEntity>
    fun findByCategoryIdInOrGeneralCategoryId(categoryIds: Collection<UUID>, generalCategoryId: UUID): List<TransactionEntity>
    fun findByGeneralCategoryId(generalCategoryId: UUID): List<TransactionEntity>
    fun findByCategoryIdIsNotNullAndGeneralCategoryIdIsNull(): List<TransactionEntity>
    fun clearCategoryId(categoryId: UUID)
    fun clearCategoryReferences(categoryId: UUID)
    fun fallbackSpecificCategoryToGeneral(specificCategoryId: UUID, generalCategoryId: UUID)
    fun updateGeneralCategoryForCategory(categoryId: UUID, newGeneralCategoryId: UUID)
    fun reassignCategory(fromCategoryId: UUID, toCategoryId: UUID)
    fun findByDestinationAccountId(destinationAccountId: UUID): List<TransactionEntity>
    fun findByCreditCardId(creditCardId: UUID): List<TransactionEntity>
    fun findByCreditCardInvoiceId(creditCardInvoiceId: UUID): List<TransactionEntity>
    fun findByResponsibleId(responsibleId: UUID): List<TransactionEntity>
    fun delete(id: UUID)
    fun deleteAll(ids: List<UUID>)
    fun saveAll(transactions: List<TransactionEntity>): List<TransactionEntity>
    fun findLatestInfiniteTransactions(): List<TransactionEntity>
    fun findFutureTransactionsByGroupId(groupId: UUID, date: LocalDate): List<TransactionEntity>
    fun findByAccountIdInAndDateBetween(accountIds: List<UUID>, startDate: LocalDate, endDate: LocalDate): List<TransactionEntity>
    fun findByResponsibleIdAndDateBetween(responsibleId: UUID, startDate: LocalDate, endDate: LocalDate): List<TransactionEntity>
}
