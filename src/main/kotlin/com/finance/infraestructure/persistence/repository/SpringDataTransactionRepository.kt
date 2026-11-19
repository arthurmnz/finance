package com.finance.infraestructure.persistence.repository

import com.finance.infraestructure.persistence.entity.TransactionJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

interface SpringDataTransactionRepository : JpaRepository<TransactionJpaEntity, UUID> {
    fun findByAccountId(accountId: UUID): List<TransactionJpaEntity>
    fun findByCategoryId(categoryId: UUID): List<TransactionJpaEntity>
    fun findByCategoryIdIn(categoryIds: Collection<UUID>): List<TransactionJpaEntity>
    fun findByCategoryIdInOrGeneralCategoryId(categoryIds: Collection<UUID>, generalCategoryId: UUID): List<TransactionJpaEntity>
    fun findByGeneralCategoryId(generalCategoryId: UUID): List<TransactionJpaEntity>
    fun findByCategoryIdIsNotNullAndGeneralCategoryIdIsNull(): List<TransactionJpaEntity>
    fun findByDestinationAccountId(destinationAccountId: UUID): List<TransactionJpaEntity>
    fun findByCreditCardId(creditCardId: UUID): List<TransactionJpaEntity>
    fun findByCreditCardInvoiceId(creditCardInvoiceId: UUID): List<TransactionJpaEntity>
    fun findByResponsibleId(responsibleId: UUID): List<TransactionJpaEntity>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TransactionJpaEntity t SET t.categoryId = null, t.updatedAt = :now WHERE t.categoryId = :categoryId")
    fun clearCategoryId(@Param("categoryId") categoryId: UUID, @Param("now") now: LocalDateTime = LocalDateTime.now()): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TransactionJpaEntity t SET t.categoryId = null, t.generalCategoryId = null, t.updatedAt = :now WHERE t.categoryId = :categoryId OR t.generalCategoryId = :categoryId")
    fun clearCategoryReferences(@Param("categoryId") categoryId: UUID, @Param("now") now: LocalDateTime = LocalDateTime.now()): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TransactionJpaEntity t SET t.categoryId = :generalCategoryId, t.generalCategoryId = :generalCategoryId, t.updatedAt = :now WHERE t.categoryId = :specificCategoryId")
    fun fallbackSpecificCategoryToGeneral(
        @Param("specificCategoryId") specificCategoryId: UUID,
        @Param("generalCategoryId") generalCategoryId: UUID,
        @Param("now") now: LocalDateTime = LocalDateTime.now()
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TransactionJpaEntity t SET t.generalCategoryId = :newGeneralCategoryId, t.updatedAt = :now WHERE t.categoryId = :categoryId")
    fun updateGeneralCategoryForCategory(
        @Param("categoryId") categoryId: UUID,
        @Param("newGeneralCategoryId") newGeneralCategoryId: UUID,
        @Param("now") now: LocalDateTime = LocalDateTime.now()
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TransactionJpaEntity t SET t.categoryId = :toCategoryId, t.updatedAt = :now WHERE t.categoryId = :fromCategoryId")
    fun reassignCategory(
        @Param("fromCategoryId") fromCategoryId: UUID,
        @Param("toCategoryId") toCategoryId: UUID,
        @Param("now") now: LocalDateTime = LocalDateTime.now()
    ): Int

    @Query("""
        SELECT t FROM TransactionJpaEntity t
        WHERE t.isInfinite = true
        AND t.date = (SELECT MAX(t2.date) FROM TransactionJpaEntity t2 WHERE t2.recurrenceGroupId = t.recurrenceGroupId)
    """)
    fun findLatestInfiniteTransactions(): List<TransactionJpaEntity>

    @Query("""
        SELECT t FROM TransactionJpaEntity t
        WHERE t.recurrenceGroupId = :groupId
        AND t.date > :date
    """)
    fun findFutureTransactionsByGroupId(groupId: UUID, date: LocalDate): List<TransactionJpaEntity>

    fun findByAccountIdInAndDateBetween(accountIds: List<UUID>, startDate: LocalDate, endDate: LocalDate): List<TransactionJpaEntity>
    fun findByResponsibleIdAndDateBetween(responsibleId: UUID, startDate: LocalDate, endDate: LocalDate): List<TransactionJpaEntity>
}
