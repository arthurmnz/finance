package com.finance.application.use_case.account

import com.finance.application.service.GroupService
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.repository.AccountGroupRequestRepository
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.JointAccountMemberRepository
import com.finance.domain.repository.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DeleteJointAccountUseCase(
    private val groupService: GroupService,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val jointAccountMemberRepository: JointAccountMemberRepository,
    private val accountGroupRequestRepository: AccountGroupRequestRepository
) {

    @Transactional
    fun execute(groupId: UUID, accountId: UUID, authenticatedUserId: UUID) {
        groupService.findGroupById(groupId)
        val membership = groupService.findMember(groupId, authenticatedUserId)
        groupService.requireOwnerOrAdmin(membership)

        val account = accountRepository.findById(accountId) ?: throw AccountNotFoundException()
        if (!account.isJoint || account.groupId != groupId) {
            throw AccountNotFoundException("Conta conjunta não encontrada neste grupo.")
        }

        AccountDeletionHelper.deleteAccountWithCascade(
            accountId = accountId,
            accountRepository = accountRepository,
            transactionRepository = transactionRepository,
            accountGroupRequestRepository = accountGroupRequestRepository,
            jointAccountMemberRepository = jointAccountMemberRepository
        )
    }
}
