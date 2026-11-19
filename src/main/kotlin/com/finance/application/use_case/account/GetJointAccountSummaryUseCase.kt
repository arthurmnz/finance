package com.finance.application.use_case.account

import com.finance.application.dto.account.JointAccountMemberShareResponse
import com.finance.application.dto.account.JointAccountSummaryResponse
import com.finance.application.mapper.AccountDtoMapper
import com.finance.application.service.AccountAccessService
import com.finance.application.service.GroupService
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.JointAccountMemberRepository
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class GetJointAccountSummaryUseCase(
    private val groupService: GroupService,
    private val groupMemberRepository: GroupMemberRepository,
    private val accountRepository: AccountRepository,
    private val jointAccountMemberRepository: JointAccountMemberRepository,
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository,
    private val accountAccessService: AccountAccessService,
    private val accountDtoMapper: AccountDtoMapper
) {

    @Transactional(readOnly = true)
    fun execute(
        groupId: UUID,
        accountId: UUID,
        month: Int? = null,
        year: Int? = null,
        userId: UUID
    ): JointAccountSummaryResponse {
        if (month != null && month !in 1..12) {
            throw IllegalArgumentException("O mês deve estar entre 1 e 12.")
        }
        if (year != null && year !in 1970..2100) {
            throw IllegalArgumentException("O ano informado é inválido.")
        }

        val now = LocalDate.now()
        val targetMonth = month ?: now.monthValue
        val targetYear = year ?: now.year

        groupService.findGroupById(groupId)
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw AccessDeniedException("Você não tem acesso a este grupo.")
        }

        val account = accountRepository.findById(accountId) ?: throw AccountNotFoundException()
        if (!account.isJoint || account.groupId != groupId) {
            throw AccountNotFoundException("Conta conjunta não encontrada neste grupo.")
        }

        accountAccessService.requireAccess(account, userId, "Você não tem acesso a esta conta conjunta.")

        val ownTxsInPeriod = transactionRepository.findByAccountId(account.id)
            .filter { it.date.monthValue == targetMonth && it.date.year == targetYear }

        val expensesInPeriod = ownTxsInPeriod.filter { it.type == TransactionType.EXPENSE }
        val completedExpenses = expensesInPeriod
            .filter { it.status == TransactionStatus.COMPLETED }
            .sumOf { it.amount.toLong() }
        val pendingExpenses = expensesInPeriod
            .filter { it.status == TransactionStatus.PENDING }
            .sumOf { it.amount.toLong() }
        val totalExpenses = completedExpenses + pendingExpenses

        val totalIncome = ownTxsInPeriod
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount.toLong() }

        val incomingTransfersInPeriod = transactionRepository.findByDestinationAccountId(account.id)
            .filter {
                it.type == TransactionType.TRANSFER &&
                    it.status == TransactionStatus.COMPLETED &&
                    it.date.monthValue == targetMonth &&
                    it.date.year == targetYear
            }
        val totalContributions = incomingTransfersInPeriod.sumOf { it.amount.toLong() }

        val memberUserIds = jointAccountMemberRepository.findAllByAccountId(account.id)
            .map { it.userId }
            .distinct()
        val sortedMembers = userRepository.findAllByIdIn(memberUserIds)
            .sortedBy { it.id }
        val memberCount = sortedMembers.size

        val sourceAccountIds = incomingTransfersInPeriod.map { it.accountId }.distinct()
        val sourceAccountsById = if (sourceAccountIds.isEmpty()) {
            emptyMap()
        } else {
            accountRepository.findAllByIdIn(sourceAccountIds).associateBy { it.id }
        }

        val contributionsByMember = mutableMapOf<UUID, Long>()
        for (tx in incomingTransfersInPeriod) {
            val sourceAccount = sourceAccountsById[tx.accountId]
            val contributorId = sourceAccount?.ownerId ?: tx.responsibleId
            contributionsByMember[contributorId] = (contributionsByMember[contributorId] ?: 0L) + tx.amount.toLong()
        }

        val launchedExpensesByMember = mutableMapOf<UUID, Long>()
        for (tx in expensesInPeriod) {
            launchedExpensesByMember[tx.responsibleId] =
                (launchedExpensesByMember[tx.responsibleId] ?: 0L) + tx.amount.toLong()
        }

        val memberShares = if (memberCount == 0) {
            emptyList()
        } else {
            val baseShare = totalExpenses / memberCount
            val remainder = (totalExpenses % memberCount).toInt()
            sortedMembers.mapIndexed { index, memberUser ->
                val shareAmount = baseShare + if (index < remainder) 1L else 0L
                val contributedAmount = contributionsByMember[memberUser.id] ?: 0L
                val launchedExpensesAmount = launchedExpensesByMember[memberUser.id] ?: 0L
                JointAccountMemberShareResponse(
                    user = accountDtoMapper.toUserSummary(memberUser),
                    shareAmount = shareAmount,
                    contributedAmount = contributedAmount,
                    launchedExpensesAmount = launchedExpensesAmount,
                    netBalance = contributedAmount - shareAmount
                )
            }
        }

        return JointAccountSummaryResponse(
            accountId = account.id,
            accountName = account.name,
            groupId = groupId,
            currentBalance = account.balance.toLong(),
            month = targetMonth,
            year = targetYear,
            totalExpenses = totalExpenses,
            completedExpenses = completedExpenses,
            pendingExpenses = pendingExpenses,
            totalIncome = totalIncome,
            totalContributions = totalContributions,
            memberCount = memberCount,
            memberShares = memberShares
        )
    }
}
