package com.finance.application.use_case.account

import com.finance.application.service.AccountAccessService
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.repository.AccountGroupRequestRepository
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.JointAccountMemberRepository
import com.finance.domain.repository.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DeleteAccountUseCase(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val accountGroupRequestRepository: AccountGroupRequestRepository,
    private val jointAccountMemberRepository: JointAccountMemberRepository,
    private val accountAccessService: AccountAccessService
) {

    @Transactional
    fun execute(accountId: UUID, authenticatedUserId: UUID) {
        val account = accountRepository.findById(accountId) ?: throw AccountNotFoundException()

        accountAccessService.requireManage(account, authenticatedUserId, "Você não tem permissão para excluir esta conta.")

        AccountDeletionHelper.deleteAccountWithCascade(
            accountId = accountId,
            accountRepository = accountRepository,
            transactionRepository = transactionRepository,
            accountGroupRequestRepository = accountGroupRequestRepository,
            jointAccountMemberRepository = jointAccountMemberRepository
        )
    }
}
