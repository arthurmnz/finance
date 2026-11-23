package com.finance.application.use_case.transaction

import com.finance.application.dto.transaction.TransactionResponse
import com.finance.application.mapper.CategoryBatchLoader
import com.finance.application.mapper.TransactionDtoMapper
import com.finance.application.service.AccountAccessService
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.exception.TransactionNotFoundException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.CategoryRepository
import com.finance.domain.repository.CreditCardRepository
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ListInstallmentGroupUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val creditCardRepository: CreditCardRepository,
    private val accountAccessService: AccountAccessService,
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository
) {
    @Transactional(readOnly = true)
    fun execute(recurrenceGroupId: UUID, authenticatedUserId: UUID): List<TransactionResponse> {
        val transactions = transactionRepository
            .findByRecurrenceGroupIdOrderByInstallmentNumberAscDateAsc(recurrenceGroupId)
        if (transactions.isEmpty()) {
            throw TransactionNotFoundException("Grupo de parcelamento não encontrado.")
        }

        val orderedTransactions = transactions.sortedWith(
            compareBy<TransactionEntity, Int?>(nullsLast()) { it.installmentNumber }
                .thenBy { it.date }
        )

        for (tx in orderedTransactions) {
            accountAccessService.requireTransactionReadAccess(
                transaction = tx,
                userId = authenticatedUserId,
                accountRepository = accountRepository,
                creditCardRepository = creditCardRepository
            )
        }

        val users = userRepository.findAll().associateBy { it.id }
        val categoryMap = CategoryBatchLoader.loadForTransactions(categoryRepository, orderedTransactions)

        return orderedTransactions.map { tx ->
            val cat = tx.categoryId?.let { categoryMap[it] }
            val parent = cat?.parentId?.let { categoryMap[it] }
            val generalCat = tx.generalCategoryId?.let { categoryMap[it] }
            TransactionDtoMapper.toResponse(tx, users[tx.responsibleId], cat, parent, generalCat)
        }
    }
}
