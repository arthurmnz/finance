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
    fun execute(categoryId: UUID): List<TransactionResponse> {
        val transactions = transactionRepository.findByCategoryId(categoryId)
        return transactions.map { TransactionDtoMapper.toResponse(it) }
    }
}
