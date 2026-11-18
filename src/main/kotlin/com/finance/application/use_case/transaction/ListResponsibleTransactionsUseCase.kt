package com.finance.application.use_case.transaction

import com.finance.application.dto.transaction.TransactionResponse
import com.finance.application.mapper.CategoryBatchLoader
import com.finance.application.mapper.TransactionDtoMapper
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ListResponsibleTransactionsUseCase(
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository
) {
    @Transactional(readOnly = true)
    fun execute(userId: UUID, month: Int?, year: Int?): List<TransactionResponse> {
        val user = userRepository.findById(userId)
        val allResponsibleTxs = transactionRepository.findByResponsibleId(userId)

        val filtered = allResponsibleTxs.filter { tx ->
            val matchMonth = month == null || tx.date.monthValue == month
            val matchYear = year == null || tx.date.year == year
            matchMonth && matchYear
        }

        val categoryIds = filtered.mapNotNull { it.categoryId }.distinct()
        val (categoryMap, parentMap) = CategoryBatchLoader.loadWithParents(categoryRepository, categoryIds)

        return filtered
            .sortedByDescending { it.date }
            .map { tx ->
                val cat = categoryMap[tx.categoryId]
                val parent = cat?.parentId?.let { parentMap[it] }
                TransactionDtoMapper.toResponse(tx, user, cat, parent)
            }
    }
}
