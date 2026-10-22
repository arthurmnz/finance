package com.finance.application.use_case.account

import com.finance.domain.enum.TransactionStatus
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.repository.AccountGroupRequestRepository
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DeleteAccountUseCase(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val accountGroupRequestRepository: AccountGroupRequestRepository
) {

    @Transactional
    fun execute(accountId: UUID, authenticatedUserId: UUID) {
        val account = accountRepository.findById(accountId) ?: throw AccountNotFoundException()

        if (account.ownerId != authenticatedUserId) {
            throw AccessDeniedException("Você só pode excluir suas próprias contas.")
        }

        // Transferências recebidas de outras contas: devolve o valor à conta de origem antes de remover.
        val incomingTransfers = transactionRepository.findByDestinationAccountId(accountId)
        for (tx in incomingTransfers) {
            if (tx.status == TransactionStatus.COMPLETED) {
                accountRepository.findById(tx.accountId)?.let {
                    it.addBalance(tx.amount)
                    accountRepository.save(it)
                }
            }
        }
        transactionRepository.deleteAll(incomingTransfers.map { it.id })

        // Transações da própria conta (o saldo da conta deixa de existir).
        val ownTransactions = transactionRepository.findByAccountId(accountId)
        transactionRepository.deleteAll(ownTransactions.map { it.id })

        accountGroupRequestRepository.deleteAllByAccountId(accountId)
        accountRepository.deleteById(accountId)
    }
}
