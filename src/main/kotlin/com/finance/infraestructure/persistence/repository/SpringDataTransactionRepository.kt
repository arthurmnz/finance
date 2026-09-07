package com.finance.infraestructure.persistence.repository

import com.finance.infraestructure.persistence.entity.TransactionJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataTransactionRepository : JpaRepository<TransactionJpaEntity, UUID> {
    fun findByAccountId(accountId: UUID): List<TransactionJpaEntity>
    fun findByCategoryId(categoryId: UUID): List<TransactionJpaEntity>
    
    @org.springframework.data.jpa.repository.Query("""
        SELECT t FROM TransactionJpaEntity t
        WHERE t.isInfinite = true
        AND t.date = (SELECT MAX(t2.date) FROM TransactionJpaEntity t2 WHERE t2.recurrenceGroupId = t.recurrenceGroupId)
    """)
    fun findLatestInfiniteTransactions(): List<TransactionJpaEntity>

    @org.springframework.data.jpa.repository.Query("""
        SELECT t FROM TransactionJpaEntity t
        WHERE t.recurrenceGroupId = :groupId
        AND t.date > :date
    """)
    fun findFutureTransactionsByGroupId(groupId: UUID, date: java.time.LocalDateTime): List<TransactionJpaEntity>
}
