package com.finance.application.use_case.transaction

import com.finance.application.dto.transaction.CreateTransactionRequest
import com.finance.application.dto.transaction.TransactionResponse
import com.finance.application.mapper.TransactionDtoMapper
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.repository.AccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CreateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    @Transactional
    fun execute(userId: UUID, request: CreateTransactionRequest): TransactionResponse {
        val transaction = TransactionEntity(
            title = request.title,
            amount = request.amount,
            date = request.date,
            responsibleId = userId,
            accountId = request.accountId,
            type = request.type,
            destinationAccountId = request.destinationAccountId,
            categoryId = request.categoryId,
            status = request.status
        )

        // Balance logic if transaction is COMPLETED
        if (transaction.status == TransactionStatus.COMPLETED) {
            val account = accountRepository.findById(request.accountId) 
                ?: throw AccountNotFoundException("Account not found")

            when (transaction.type) {
                TransactionType.INCOME -> account.addBalance(transaction.amount)
                TransactionType.EXPENSE -> account.subtractBalance(transaction.amount)
                TransactionType.TRANSFER -> {
                    account.subtractBalance(transaction.amount)
                    val destinationAccount = request.destinationAccountId?.let { accountRepository.findById(it) }
                        ?: throw AccountNotFoundException("Destination account not found")
                    destinationAccount.addBalance(transaction.amount)
                    accountRepository.save(destinationAccount)
                }
            }
            accountRepository.save(account)
        }

        val savedTransaction = transactionRepository.save(transaction)
        return TransactionDtoMapper.toResponse(savedTransaction)
    }
}
