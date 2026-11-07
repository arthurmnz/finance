package com.finance.application.use_case.transaction

import com.finance.application.dto.transaction.TransactionResponse
import com.finance.application.mapper.TransactionDtoMapper
import com.finance.domain.entity.CreditCardEntity
import com.finance.domain.repository.CreditCardRepository
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth
import java.util.UUID

@Service
class ListResponsibleTransactionsUseCase(
    private val transactionRepository: TransactionRepository,
    private val creditCardRepository: CreditCardRepository,
    private val userRepository: UserRepository
) {
    @Transactional(readOnly = true)
    fun execute(userId: UUID, month: Int?, year: Int?): List<TransactionResponse> {
        val user = userRepository.findById(userId)
        val allResponsibleTxs = transactionRepository.findByResponsibleId(userId)

        val cardCache = mutableMapOf<UUID, CreditCardEntity?>()

        val filtered = if (month != null && year != null) {
            val targetYm = YearMonth.of(year, month)
            allResponsibleTxs.filter { tx ->
                if (tx.creditCardId != null) {
                    if (tx.invoiceMonth != null && tx.invoiceYear != null) {
                        tx.invoiceMonth == month && tx.invoiceYear == year
                    } else {
                        val card = cardCache.computeIfAbsent(tx.creditCardId!!) {
                            creditCardRepository.findById(it)
                        }
                        val txYm = card?.defaultInvoiceMonthYear(tx.date) ?: YearMonth.from(tx.date)
                        txYm == targetYm
                    }
                } else {
                    tx.date.monthValue == month && tx.date.year == year
                }
            }
        } else {
            allResponsibleTxs
        }

        return filtered
            .sortedByDescending { it.date }
            .map { TransactionDtoMapper.toResponse(it, user) }
    }
}
