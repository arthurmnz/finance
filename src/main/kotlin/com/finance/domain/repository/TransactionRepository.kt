package com.finance.domain.repository

import com.finance.domain.entity.TransactionEntity
import java.time.LocalDate
import java.util.UUID

interface TransactionRepository {
    fun save(transaction: TransactionEntity): TransactionEntity
    fun findById(id: UUID): TransactionEntity?
    fun findByAccountId(accountId: UUID): List<TransactionEntity>
    fun findByCategoryId(categoryId: UUID): List<TransactionEntity>
    fun delete(id: UUID)
    fun deleteAll(ids: List<UUID>)
    fun saveAll(transactions: List<TransactionEntity>): List<TransactionEntity>
    fun findLatestInfiniteTransactions(): List<TransactionEntity>
    fun findFutureTransactionsByGroupId(groupId: UUID, date: LocalDate): List<TransactionEntity>
    fun findByAccountIdInAndDateBetween(accountIds: List<UUID>, startDate: LocalDate, endDate: LocalDate): List<TransactionEntity>
}
