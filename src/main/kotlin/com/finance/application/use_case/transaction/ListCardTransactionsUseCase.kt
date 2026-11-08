package com.finance.application.use_case.transaction

import com.finance.application.dto.transaction.TransactionResponse
import com.finance.application.mapper.TransactionDtoMapper
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.CreditCardNotFoundException
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.CreditCardRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ListCardTransactionsUseCase(
    private val transactionRepository: TransactionRepository,
    private val creditCardRepository: CreditCardRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository
) {
    @Transactional(readOnly = true)
    fun execute(cardId: UUID, month: Int? = null, year: Int? = null, authenticatedUserId: UUID? = null): List<TransactionResponse> {
        month?.let {
            if (month !in 1..12)
                throw IllegalArgumentException("O mês deve estar entre 1 e 12.")
        }

        if (authenticatedUserId != null) {
            val card = creditCardRepository.findById(cardId) ?: throw CreditCardNotFoundException()
            val allowed = card.ownerId == authenticatedUserId ||
                (card.groupId != null && groupMemberRepository.existsByGroupIdAndUserId(card.groupId!!, authenticatedUserId))
            if (!allowed) {
                throw AccessDeniedException("Você não tem acesso às transações deste cartão de crédito.")
            }
        }

        val transactions = transactionRepository.findByCreditCardId(cardId)

        val filteredTransactions = transactions.filter {
            (month == null || it.date.monthValue == month) &&
            (year == null || it.date.year == year)
        }

        val users = userRepository.findAll().associateBy { it.id }
        val categoryIds = filteredTransactions.mapNotNull { it.categoryId }.distinct()
        val categoryMap = categoryIds.mapNotNull { categoryRepository.findById(it) }.associateBy { it.id }

        return filteredTransactions
            .sortedByDescending { it.date }
            .map { TransactionDtoMapper.toResponse(it, users[it.responsibleId], categoryMap[it.categoryId]) }
    }
}
