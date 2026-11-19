package com.finance.application.use_case.account

import com.finance.domain.enum.TransactionStatus
import com.finance.domain.repository.AccountGroupRequestRepository
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.JointAccountMemberRepository
import com.finance.domain.repository.TransactionRepository
import java.util.UUID

/**
 * Helper compartilhado para exclusão em cascata de uma conta (individual ou conjunta),
 * revertendo saldos de transferências recebidas e limpando vínculos/transações.
 */
object AccountDeletionHelper {
    fun deleteAccountWithCascade(
        accountId: UUID,
        accountRepository: AccountRepository,
        transactionRepository: TransactionRepository,
        accountGroupRequestRepository: AccountGroupRequestRepository,
        jointAccountMemberRepository: JointAccountMemberRepository
    ) {
        val incomingTransfers = transactionRepository.findByDestinationAccountId(accountId)
        for (tx in incomingTransfers) {
            if (tx.status == TransactionStatus.COMPLETED && tx.accountId != accountId) {
                accountRepository.findById(tx.accountId)?.let { sourceAccount ->
                    sourceAccount.addBalance(tx.amount)
                    accountRepository.save(sourceAccount)
                }
            }
        }
        val incomingIds = incomingTransfers.map { it.id }
        if (incomingIds.isNotEmpty()) {
            transactionRepository.deleteAll(incomingIds)
        }

        val ownTransactions = transactionRepository.findByAccountId(accountId)
        val ownIds = ownTransactions.map { it.id }
        if (ownIds.isNotEmpty()) {
            transactionRepository.deleteAll(ownIds)
        }

        jointAccountMemberRepository.deleteAllByAccountId(accountId)
        accountGroupRequestRepository.deleteAllByAccountId(accountId)
        accountRepository.deleteById(accountId)
    }
}
