package com.finance.application.use_case.transaction

import com.finance.application.dto.transaction.TransactionResponse
import com.finance.application.mapper.TransactionDtoMapper
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.CategoryNotFoundException
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ListCategoryTransactionsUseCase(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val userRepository: UserRepository
) {
    @Transactional(readOnly = true)
    fun execute(categoryId: UUID, month: Int? = null, year: Int? = null, authenticatedUserId: UUID? = null): List<TransactionResponse> {
        month?.let {
            if (month !in 1..12)
                throw IllegalArgumentException("O mês deve estar entre 1 e 12.")
        }

        if (authenticatedUserId != null) {
            val category = categoryRepository.findById(categoryId) ?: throw CategoryNotFoundException()
            val allowed = category.userId == authenticatedUserId ||
                (category.groupId != null && groupMemberRepository.existsByGroupIdAndUserId(category.groupId!!, authenticatedUserId))
            if (!allowed) {
                throw AccessDeniedException("Você não tem acesso às transações desta categoria.")
            }
        }

        val transactions = transactionRepository.findByCategoryId(categoryId)

        val filteredTransactions = transactions.filter {
            (month == null || it.date.monthValue == month) &&
            (year == null || it.date.year == year)
        }

        val users = userRepository.findAll().associateBy { it.id }
        return filteredTransactions.map { TransactionDtoMapper.toResponse(it, users[it.responsibleId]) }
    }
}
