package com.finance.application.use_case.group

import com.finance.application.dto.group.DebtSettlementResponse
import com.finance.application.dto.group.ForgiveDebtRequest
import com.finance.application.dto.group.GroupUserSummary
import com.finance.domain.entity.DebtSettlementEntity
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.GroupNotFoundException
import com.finance.domain.exception.InvalidTransactionException
import com.finance.domain.repository.DebtSettlementRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
import com.finance.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class ForgiveGroupDebtUseCase(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val debtSettlementRepository: DebtSettlementRepository,
    private val userRepository: UserRepository,
    private val calculateGroupDebtsUseCase: CalculateGroupDebtsUseCase
) {
    @Transactional
    fun execute(groupId: UUID, userId: UUID, request: ForgiveDebtRequest): DebtSettlementResponse {
        if (request.amount <= 0) {
            throw InvalidTransactionException("O valor do perdão deve ser maior que zero.")
        }
        if (request.debtorId == userId) {
            throw InvalidTransactionException("Você não pode perdoar uma dívida consigo mesmo.")
        }
        if (request.targetMonth !in 1..12) {
            throw InvalidTransactionException("Mês alvo inválido.")
        }
        if (request.targetYear !in 1970..2100) {
            throw InvalidTransactionException("Ano alvo inválido.")
        }

        val cleanNotes = request.notes?.trim()?.takeIf { it.isNotEmpty() }
        if (cleanNotes != null && cleanNotes.length > 500) {
            throw InvalidTransactionException("A observação deve ter no máximo 500 caracteres.")
        }

        groupRepository.findById(groupId) ?: throw GroupNotFoundException()

        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw AccessDeniedException("Você não pertence a este grupo.")
        }
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, request.debtorId)) {
            throw AccessDeniedException("O devedor informado não pertence a este grupo.")
        }

        val debts = calculateGroupDebtsUseCase.execute(groupId, userId)
        val monthly = debts.monthlyBalances.find {
            it.month == request.targetMonth && it.year == request.targetYear
        }
        val pairBalance = monthly?.userBalances?.find {
            it.debtor.id == request.debtorId && it.creditor.id == userId
        }

        if (pairBalance == null || pairBalance.netAmount <= 0) {
            throw InvalidTransactionException("Não há saldo devedor pendente deste membro para você neste período.")
        }
        if (request.amount > pairBalance.netAmount) {
            throw InvalidTransactionException("O valor do perdão não pode exceder o saldo devedor pendente.")
        }

        val settlement = DebtSettlementEntity(
            groupId = groupId,
            debtorId = request.debtorId,
            creditorId = userId,
            amount = request.amount,
            targetMonth = request.targetMonth,
            targetYear = request.targetYear,
            date = LocalDate.now(),
            transactionId = null,
            notes = cleanNotes ?: "Perdão de dívida",
            isForgiveness = true
        )
        val savedSettlement = debtSettlementRepository.save(settlement)

        val debtorUser = userRepository.findById(request.debtorId)
        val creditorUser = userRepository.findById(userId)

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
            isForgiveness = savedSettlement.isForgiveness,
            createdAt = savedSettlement.createdAt
        )
    }
}
