package com.finance.application.use_case.group

import com.finance.application.dto.group.DebtSettlementResponse
import com.finance.application.dto.group.GroupUserSummary
import com.finance.application.dto.group.SettleDebtRequest
import com.finance.domain.entity.DebtSettlementEntity
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.exception.GroupNotFoundException
import com.finance.domain.exception.InvalidTransactionException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.DebtSettlementRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class SettleGroupDebtUseCase(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val debtSettlementRepository: DebtSettlementRepository,
    private val userRepository: UserRepository
) {
    @Transactional
    fun execute(groupId: UUID, userId: UUID, request: SettleDebtRequest): DebtSettlementResponse {
        if (request.amount <= 0) {
            throw InvalidTransactionException("O valor do acerto deve ser maior que zero.")
        }
        if (request.creditorId == userId) {
            throw InvalidTransactionException("Você não pode realizar um acerto com você mesmo.")
        }
        if (request.targetMonth !in 1..12) {
            throw InvalidTransactionException("Mês alvo inválido.")
        }

        val group = groupRepository.findById(groupId) ?: throw GroupNotFoundException()

        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw AccessDeniedException("Você não pertence a este grupo.")
        }
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, request.creditorId)) {
            throw AccessDeniedException("O credor informado não pertence a este grupo.")
        }

        val sourceAccount = accountRepository.findById(request.sourceAccountId)
            ?: throw AccountNotFoundException("Conta de origem não encontrada.")
        val destAccount = accountRepository.findById(request.destinationAccountId)
            ?: throw AccountNotFoundException("Conta de destino não encontrada.")

        if (sourceAccount.ownerId != userId) {
            throw AccessDeniedException("A conta de origem deve pertencer a você.")
        }
        if (destAccount.ownerId != request.creditorId) {
            throw AccessDeniedException("A conta de destino deve pertencer ao credor.")
        }

        sourceAccount.subtractBalance(request.amount)
        destAccount.addBalance(request.amount)
        accountRepository.save(sourceAccount)
        accountRepository.save(destAccount)

        val transaction = TransactionEntity(
            title = "Acerto de cartão - ${group.name} (${request.targetMonth}/${request.targetYear})",
            amount = request.amount,
            date = LocalDate.now(),
            responsibleId = userId,
            accountId = sourceAccount.id,
            type = TransactionType.TRANSFER,
            destinationAccountId = destAccount.id,
            categoryId = null,
            status = TransactionStatus.COMPLETED
        )
        val savedTx = transactionRepository.save(transaction)

        val settlement = DebtSettlementEntity(
            groupId = groupId,
            debtorId = userId,
            creditorId = request.creditorId,
            amount = request.amount,
            targetMonth = request.targetMonth,
            targetYear = request.targetYear,
            date = LocalDate.now(),
            transactionId = savedTx.id,
            notes = request.notes
        )
        val savedSettlement = debtSettlementRepository.save(settlement)

        val debtorUser = userRepository.findById(userId)
        val creditorUser = userRepository.findById(request.creditorId)

        return DebtSettlementResponse(
            id = savedSettlement.id,
            groupId = savedSettlement.groupId,
            debtorId = savedSettlement.debtorId,
            debtor = debtorUser?.let {
                GroupUserSummary(it.id, it.firstName.value, it.lastName.value, it.email.normalized)
            },
            creditorId = savedSettlement.creditorId,
            creditor = creditorUser?.let {
                GroupUserSummary(it.id, it.firstName.value, it.lastName.value, it.email.normalized)
            },
            amount = savedSettlement.amount,
            targetMonth = savedSettlement.targetMonth,
            targetYear = savedSettlement.targetYear,
            date = savedSettlement.date,
            transactionId = savedSettlement.transactionId,
            notes = savedSettlement.notes,
            createdAt = savedSettlement.createdAt
        )
    }
}
