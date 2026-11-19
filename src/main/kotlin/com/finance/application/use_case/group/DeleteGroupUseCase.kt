package com.finance.application.use_case.group

import com.finance.application.service.GroupService
import com.finance.application.use_case.account.AccountDeletionHelper
import com.finance.domain.repository.AccountGroupRequestRepository
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.GroupInvitationRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
import com.finance.domain.repository.JointAccountMemberRepository
import com.finance.domain.repository.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DeleteGroupUseCase(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val groupInvitationRepository: GroupInvitationRepository,
    private val groupService: GroupService,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val jointAccountMemberRepository: JointAccountMemberRepository,
    private val accountGroupRequestRepository: AccountGroupRequestRepository,
) {

    @Transactional
    fun execute(groupId: UUID, authenticatedUserId: UUID) {
        val group = groupService.findGroupById(groupId)
        val member = groupService.findMember(group.id, authenticatedUserId)

        groupService.requireOwner(member)

        // Cascateia a exclusão das contas conjuntas do grupo
        val jointAccounts = accountRepository.findAllByGroupIdAndIsJointTrue(group.id)
        jointAccounts.forEach { jointAccount ->
            AccountDeletionHelper.deleteAccountWithCascade(
                accountId = jointAccount.id,
                accountRepository = accountRepository,
                transactionRepository = transactionRepository,
                accountGroupRequestRepository = accountGroupRequestRepository,
                jointAccountMemberRepository = jointAccountMemberRepository
            )
        }

        // Deleta convites e membros antes do grupo (integridade referencial)
        groupInvitationRepository.deleteAllByGroupId(group.id)
        groupMemberRepository.deleteAllByGroupId(group.id)
        groupRepository.deleteById(group.id)
    }
}
