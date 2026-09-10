package com.finance.application.use_case.transaction

import com.finance.application.dto.transaction.TransactionResponse
import com.finance.application.mapper.TransactionDtoMapper
import com.finance.domain.repository.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ListCategoryTransactionsUseCase(
    private val transactionRepository: TransactionRepository
) {
    @Transactional(readOnly = true)
    fun execute(categoryId: UUID, month: Int? = null, year: Int? = null): List<TransactionResponse> {
        month?.let {
            if (month !in 1..12)
                throw IllegalArgumentException("O mês deve estar entre 1 e 12.")
        }

        val transactions = transactionRepository.findByCategoryId(categoryId)
        
        val filteredTransactions = transactions.filter {
            (month == null || it.date.monthValue == month) &&
            (year == null || it.date.year == year)
        }

        return filteredTransactions.map { TransactionDtoMapper.toResponse(it) }
    }
}
