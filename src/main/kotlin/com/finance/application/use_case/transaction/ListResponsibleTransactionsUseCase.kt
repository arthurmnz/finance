package com.finance.application.use_case.transaction

import com.finance.application.dto.transaction.TransactionResponse
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
        val categoryMap = categoryIds.mapNotNull { categoryRepository.findById(it) }.associateBy { it.id }

        return filtered
            .sortedByDescending { it.date }
            .map { TransactionDtoMapper.toResponse(it, user, categoryMap[it.categoryId]) }
    }
}
