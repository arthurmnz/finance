package com.finance.application.use_case.group

import com.finance.application.dto.group.DebtSettlementResponse
import com.finance.application.dto.group.GroupDebtsResponse
import com.finance.application.dto.group.GroupMonthlyDebtResponse
import com.finance.application.dto.group.GroupUserSummary
import com.finance.application.dto.group.MemberDebtDetailResponse
import com.finance.application.dto.group.PairDebtBalanceResponse
import com.finance.application.dto.transaction.TransactionResponse
import com.finance.application.mapper.TransactionDtoMapper
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.GroupNotFoundException
import com.finance.domain.repository.CreditCardRepository
import com.finance.domain.repository.DebtSettlementRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth
import java.util.UUID

@Service
class CalculateGroupDebtsUseCase(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val creditCardRepository: CreditCardRepository,
    private val transactionRepository: TransactionRepository,
    private val debtSettlementRepository: DebtSettlementRepository,
    private val userRepository: UserRepository
) {
    private data class CardPurchaseItem(
        val debtorId: UUID,
        val creditorId: UUID,
        val ym: YearMonth,
        val signedAmount: Int,
        val tx: TransactionEntity,
        val txResponse: TransactionResponse
    )

    @Transactional(readOnly = true)
    fun execute(groupId: UUID, userId: UUID): GroupDebtsResponse {
        val group = groupRepository.findById(groupId) ?: throw GroupNotFoundException()
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw AccessDeniedException("Você não tem acesso a este grupo.")
        }

        val users = userRepository.findAll().associateBy { it.id }
        fun toSummary(uId: UUID): GroupUserSummary {
            val u = users[uId]
            return GroupUserSummary(
                id = uId,
                firstName = u?.firstName?.value ?: "Usuário",
                lastName = u?.lastName?.value ?: "",
                email = u?.email?.normalized ?: ""
            )
        }

        val cards = creditCardRepository.findAllByGroupId(groupId)
        val settlements = debtSettlementRepository.findByGroupId(groupId)

        val purchases = mutableListOf<CardPurchaseItem>()
        val allYms = mutableSetOf<YearMonth>()
        allYms.add(YearMonth.now())

        for (card in cards) {
            val txs = transactionRepository.findByCreditCardId(card.id)
            for (tx in txs) {
                if (tx.responsibleId != card.ownerId) {
                    val ym = if (tx.invoiceMonth != null && tx.invoiceYear != null) {
                        YearMonth.of(tx.invoiceYear!!, tx.invoiceMonth!!)
                    } else {
                        card.defaultInvoiceMonthYear(tx.date)
                    }
                    allYms.add(ym)
                    val signed = if (tx.type == TransactionType.INCOME) -tx.amount else tx.amount
                    val resp = TransactionDtoMapper.toResponse(tx, users[tx.responsibleId])
                    purchases.add(
                        CardPurchaseItem(
                            debtorId = tx.responsibleId,
                            creditorId = card.ownerId,
                            ym = ym,
                            signedAmount = signed,
                            tx = tx,
                            txResponse = resp
                        )
                    )
                }
            }
        }

        for (s in settlements) {
            allYms.add(YearMonth.of(s.targetYear, s.targetMonth))
        }

        val sortedYms = allYms.sorted()
        val monthlyResponses = mutableListOf<GroupMonthlyDebtResponse>()

        for (ym in sortedYms) {
            val userPairs = mutableSetOf<Pair<UUID, UUID>>()

            purchases.filter { it.ym == ym }.forEach { p ->
                val pair = if (p.debtorId < p.creditorId) Pair(p.debtorId, p.creditorId) else Pair(p.creditorId, p.debtorId)
                userPairs.add(pair)
            }

            settlements.filter { it.targetMonth == ym.monthValue && it.targetYear == ym.year }.forEach { s ->
                val pair = if (s.debtorId < s.creditorId) Pair(s.debtorId, s.creditorId) else Pair(s.creditorId, s.debtorId)
                userPairs.add(pair)
            }

            val pairBalances = mutableListOf<PairDebtBalanceResponse>()

            for ((uA, uB) in userPairs) {
                val txsAtoB = purchases.filter { it.debtorId == uA && it.creditorId == uB && it.ym == ym }
                val grossAtoB = txsAtoB.sumOf { it.signedAmount }.coerceAtLeast(0)
                val settledAtoB = settlements.filter {
                    it.debtorId == uA && it.creditorId == uB && it.targetMonth == ym.monthValue && it.targetYear == ym.year
                }.sumOf { it.amount }
                val remainingAtoB = (grossAtoB - settledAtoB).coerceAtLeast(0)

                val txsBtoA = purchases.filter { it.debtorId == uB && it.creditorId == uA && it.ym == ym }
                val grossBtoA = txsBtoA.sumOf { it.signedAmount }.coerceAtLeast(0)
                val settledBtoA = settlements.filter {
                    it.debtorId == uB && it.creditorId == uA && it.targetMonth == ym.monthValue && it.targetYear == ym.year
                }.sumOf { it.amount }
                val remainingBtoA = (grossBtoA - settledBtoA).coerceAtLeast(0)

                if (grossAtoB == 0 && grossBtoA == 0 && settledAtoB == 0 && settledBtoA == 0) {
                    continue
                }

                val net = remainingAtoB - remainingBtoA
                val (debtorId, creditorId, netAmount) = if (net >= 0) {
                    Triple(uA, uB, net)
                } else {
                    Triple(uB, uA, -net)
                }

                val grossDebts = mutableListOf<MemberDebtDetailResponse>()
                if (txsAtoB.isNotEmpty() || settledAtoB > 0) {
                    grossDebts.add(
                        MemberDebtDetailResponse(
                            debtorId = uA,
                            creditorId = uB,
                            grossAmount = grossAtoB,
                            settledAmount = settledAtoB,
                            remainingAmount = remainingAtoB,
                            transactions = txsAtoB.map { it.txResponse }
                        )
                    )
                }
                if (txsBtoA.isNotEmpty() || settledBtoA > 0) {
                    grossDebts.add(
                        MemberDebtDetailResponse(
                            debtorId = uB,
                            creditorId = uA,
                            grossAmount = grossBtoA,
                            settledAmount = settledBtoA,
                            remainingAmount = remainingBtoA,
                            transactions = txsBtoA.map { it.txResponse }
                        )
                    )
                }

                pairBalances.add(
                    PairDebtBalanceResponse(
                        debtor = toSummary(debtorId),
                        creditor = toSummary(creditorId),
                        netAmount = netAmount,
                        grossDebts = grossDebts
                    )
                )
            }

            monthlyResponses.add(
                GroupMonthlyDebtResponse(
                    month = ym.monthValue,
                    year = ym.year,
                    userBalances = pairBalances
                )
            )
        }

        val totalOwedByMe = monthlyResponses.sumOf { m ->
            m.userBalances.filter { it.debtor.id == userId }.sumOf { it.netAmount }
        }

        val totalReceivableByMe = monthlyResponses.sumOf { m ->
            m.userBalances.filter { it.creditor.id == userId }.sumOf { it.netAmount }
        }

        val settlementResponses = settlements.map { s ->
            DebtSettlementResponse(
                id = s.id,
                groupId = s.groupId,
                debtorId = s.debtorId,
                debtor = toSummary(s.debtorId),
                creditorId = s.creditorId,
                creditor = toSummary(s.creditorId),
                amount = s.amount,
                targetMonth = s.targetMonth,
                targetYear = s.targetYear,
                date = s.date,
                transactionId = s.transactionId,
                notes = s.notes,
                createdAt = s.createdAt
            )
        }.sortedByDescending { it.createdAt }

        return GroupDebtsResponse(
            groupId = groupId,
            totalOwedByMe = totalOwedByMe,
            totalReceivableByMe = totalReceivableByMe,
            monthlyBalances = monthlyResponses,
            settlements = settlementResponses
        )
    }
}
