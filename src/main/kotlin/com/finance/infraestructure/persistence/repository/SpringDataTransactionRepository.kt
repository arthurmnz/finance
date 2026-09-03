package com.finance.infraestructure.persistence.repository

import com.finance.infraestructure.persistence.entity.TransactionJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SpringDataTransactionRepository : JpaRepository<TransactionJpaEntity, UUID> {
    fun findByAccountId(accountId: UUID): List<TransactionJpaEntity>
    fun findByCategoryId(categoryId: UUID): List<TransactionJpaEntity>
}
