package com.finance.application.dto.credit_card

import com.finance.application.dto.group.GroupUserSummary
import com.finance.application.dto.transaction.TransactionResponse
import com.finance.domain.enum.AccountGroupRequestStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class CreateCreditCardRequest(
    val name: String,
    val accountId: UUID,
    val limitAmount: Int,
    val closingDay: Int,
    val dueDay: Int
)

data class UpdateCreditCardRequest(
    val name: String,
    val accountId: UUID,
    val limitAmount: Int,
    val closingDay: Int,
    val dueDay: Int
)

data class CreditCardResponse(
    val id: UUID,
    val ownerId: UUID,
    val owner: GroupUserSummary?,
    val accountId: UUID,
    val name: String,
    val limitAmount: Int,
    val usedLimit: Int,
    val availableLimit: Int,
    val closingDay: Int,
    val dueDay: Int,
    val groupId: UUID?,
    val currentInvoiceMonth: Int,
    val currentInvoiceYear: Int,
    val currentInvoiceAmount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class CreditCardInvoiceResponse(
    val creditCardId: UUID,
    val month: Int,
    val year: Int,
    val closingDate: LocalDate,
    val dueDate: LocalDate,
    val purchasesAmount: Int,
    val carriedOverAmount: Int,
    val revolvingInterestAmount: Int,
    val totalAmount: Int,
    val paidAmount: Int,
    val remainingAmount: Int,
    val status: String,
    val transactions: List<TransactionResponse>,
    val byResponsible: List<ResponsibleSummary>
) {
    data class ResponsibleSummary(
        val user: GroupUserSummary,
        val amount: Int
    )
}

data class PayInvoiceRequest(
    val amount: Int,
    val accountId: UUID? = null,
    val date: LocalDate = LocalDate.now(),
    /** Se verdadeiro (ou se pagamento parcial e indicado), transfere o saldo não pago para a fatura do mês seguinte. */
    val rollRemainingToNextMonth: Boolean = true,
    /** Juros/encargos do rotativo a adicionar na fatura do mês seguinte (opcional). */
    val nextMonthRevolvingInterest: Int? = null
)

data class UpdateInvoiceRevolvingRequest(
    val revolvingInterestAmount: Int,
    val carriedOverAmount: Int? = null
)

data class CreditCardGroupRequestResponse(
    val id: UUID,
    val creditCard: CardSummary,
    val requester: GroupUserSummary,
    val status: AccountGroupRequestStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    data class CardSummary(
        val id: UUID,
        val name: String
    )
}
