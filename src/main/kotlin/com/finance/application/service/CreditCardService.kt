package com.finance.application.service

import com.finance.application.dto.credit_card.CreditCardGroupRequestResponse
import com.finance.application.dto.credit_card.CreditCardInvoiceResponse
import com.finance.application.dto.credit_card.CreditCardResponse
import com.finance.application.dto.credit_card.CreateCreditCardRequest
import com.finance.application.dto.credit_card.PayInvoiceRequest
import com.finance.application.dto.credit_card.UpdateCreditCardRequest
import com.finance.application.dto.credit_card.UpdateInvoiceRevolvingRequest
import com.finance.application.dto.group.GroupUserSummary
import com.finance.application.mapper.TransactionDtoMapper
import com.finance.domain.entity.CreditCardEntity
import com.finance.domain.entity.CreditCardGroupRequestEntity
import com.finance.domain.entity.CreditCardInvoiceEntity
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.entity.UserEntity
import com.finance.domain.enum.AccountGroupRequestStatus
import com.finance.domain.enum.GroupRole
import com.finance.domain.enum.TransactionStatus
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.AccountAlreadyLinkedException
import com.finance.domain.exception.AccountGroupRequestAlreadyExistsException
import com.finance.domain.exception.AccountGroupRequestNotFoundException
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.exception.CreditCardNotFoundException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.CreditCardGroupRequestRepository
import com.finance.domain.repository.CreditCardInvoiceRepository
import com.finance.domain.repository.CreditCardRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.TransactionRepository
import com.finance.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Service
class CreditCardService(
    private val creditCardRepository: CreditCardRepository,
    private val invoiceRepository: CreditCardInvoiceRepository,
    private val groupRequestRepository: CreditCardGroupRequestRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val groupService: GroupService,
    private val userRepository: UserRepository
) {

    @Transactional
    fun create(request: CreateCreditCardRequest, userId: UUID): CreditCardResponse {
        val account = accountRepository.findById(request.accountId) ?: throw AccountNotFoundException()
        if (account.ownerId != userId) {
            throw AccessDeniedException("Você só pode vincular o cartão a uma conta sua.")
        }
        val card = CreditCardEntity(
            ownerId = userId,
            accountId = request.accountId,
            name = request.name,
            limitAmount = request.limitAmount,
            closingDay = request.closingDay,
            dueDay = request.dueDay
        )
        val saved = creditCardRepository.save(card)
        return toCardResponse(saved, userCache())
    }

    @Transactional
    fun update(cardId: UUID, request: UpdateCreditCardRequest, userId: UUID): CreditCardResponse {
        val card = creditCardRepository.findById(cardId) ?: throw CreditCardNotFoundException()
        if (card.ownerId != userId) {
            throw AccessDeniedException("Você só pode editar seus próprios cartões.")
        }
        val account = accountRepository.findById(request.accountId) ?: throw AccountNotFoundException()
        if (account.ownerId != userId) {
            throw AccessDeniedException("Você só pode vincular o cartão a uma conta sua.")
        }
        card.update(
            newName = request.name,
            newAccountId = request.accountId,
            newLimitAmount = request.limitAmount,
            newClosingDay = request.closingDay,
            newDueDay = request.dueDay
        )
        val saved = creditCardRepository.save(card)
        return toCardResponse(saved, userCache())
    }

    @Transactional
    fun delete(cardId: UUID, userId: UUID) {
        val card = creditCardRepository.findById(cardId) ?: throw CreditCardNotFoundException()
        if (card.ownerId != userId) {
            throw AccessDeniedException("Você só pode excluir seus próprios cartões.")
        }
        val txs = transactionRepository.findByCreditCardId(cardId)
        transactionRepository.deleteAll(txs.map { it.id })
        invoiceRepository.deleteAllByCreditCardId(cardId)
        groupRequestRepository.deleteAllByCreditCardId(cardId)
        creditCardRepository.deleteById(cardId)
    }

    @Transactional(readOnly = true)
    fun listAccessibleCards(userId: UUID): List<CreditCardResponse> {
        val ownCards = creditCardRepository.findAllByOwnerId(userId)
        val groupIds = groupMemberRepository.findAllByUserId(userId).map { it.groupId }
        val groupCards = creditCardRepository.findAllByGroupIdIn(groupIds)
        val all = (ownCards + groupCards).distinctBy { it.id }
        val users = userCache()
        return all.map { toCardResponse(it, users) }
    }

    @Transactional(readOnly = true)
    fun listGroupCards(groupId: UUID, userId: UUID): List<CreditCardResponse> {
        groupService.findMember(groupId, userId)
        val users = userCache()
        return creditCardRepository.findAllByGroupId(groupId).map { toCardResponse(it, users) }
    }

    @Transactional
    fun linkToGroup(cardId: UUID, groupId: UUID, userId: UUID): Any {
        val card = creditCardRepository.findById(cardId) ?: throw CreditCardNotFoundException()
        if (card.ownerId != userId) {
            throw AccessDeniedException("Você só pode vincular seus próprios cartões.")
        }
        if (card.groupId != null) {
            throw AccountAlreadyLinkedException("Este cartão já está vinculado a um grupo.")
        }
        val membership = groupService.findMember(groupId, userId)
        if (membership.role == GroupRole.OWNER || membership.role == GroupRole.ADMIN) {
            card.linkToGroup(groupId)
            val saved = creditCardRepository.save(card)
            return toCardResponse(saved, userCache())
        }
        val existing = groupRequestRepository.findByCreditCardIdAndStatus(cardId, AccountGroupRequestStatus.PENDING)
        if (existing != null) {
            throw AccountGroupRequestAlreadyExistsException("Já existe uma solicitação pendente para este cartão.")
        }
        return groupRequestRepository.save(
            CreditCardGroupRequestEntity(creditCardId = cardId, groupId = groupId, requesterId = userId)
        )
    }

    @Transactional
    fun unlinkFromGroup(cardId: UUID, userId: UUID) {
        val card = creditCardRepository.findById(cardId) ?: throw CreditCardNotFoundException()
        val gid = card.groupId ?: return
        if (card.ownerId != userId) {
            val membership = groupService.findMember(gid, userId)
            groupService.requireOwnerOrAdmin(membership)
        }
        card.unlinkFromGroup()
        creditCardRepository.save(card)
    }

    @Transactional(readOnly = true)
    fun listPendingGroupRequests(groupId: UUID, userId: UUID): List<CreditCardGroupRequestResponse> {
        val membership = groupService.findMember(groupId, userId)
        groupService.requireOwnerOrAdmin(membership)
        val users = userCache()
        return groupRequestRepository.findAllByGroupIdAndStatus(groupId, AccountGroupRequestStatus.PENDING).mapNotNull { req ->
            val card = creditCardRepository.findById(req.creditCardId) ?: return@mapNotNull null
            val requester = users[req.requesterId] ?: return@mapNotNull null
            CreditCardGroupRequestResponse(
                id = req.id,
                creditCard = CreditCardGroupRequestResponse.CardSummary(card.id, card.name),
                requester = toUserSummary(requester),
                status = req.status,
                createdAt = req.createdAt,
                updatedAt = req.updatedAt
            )
        }
    }

    @Transactional
    fun acceptGroupRequest(requestId: UUID, groupId: UUID, userId: UUID) {
        val req = groupRequestRepository.findById(requestId) ?: throw AccountGroupRequestNotFoundException()
        require(req.groupId == groupId) { "Solicitação não pertence a este grupo." }
        val membership = groupService.findMember(groupId, userId)
        groupService.requireOwnerOrAdmin(membership)
        req.accept()
        groupRequestRepository.save(req)
        val card = creditCardRepository.findById(req.creditCardId) ?: throw CreditCardNotFoundException()
        if (card.groupId != null) throw AccountAlreadyLinkedException("Cartão já vinculado.")
        card.linkToGroup(groupId)
        creditCardRepository.save(card)
    }

    @Transactional
    fun rejectGroupRequest(requestId: UUID, groupId: UUID, userId: UUID) {
        val req = groupRequestRepository.findById(requestId) ?: throw AccountGroupRequestNotFoundException()
        require(req.groupId == groupId) { "Solicitação não pertence a este grupo." }
        val membership = groupService.findMember(groupId, userId)
        groupService.requireOwnerOrAdmin(membership)
        req.reject()
        groupRequestRepository.save(req)
    }

    @Transactional(readOnly = true)
    fun getInvoice(cardId: UUID, month: Int, year: Int, userId: UUID): CreditCardInvoiceResponse {
        require(month in 1..12) { "Mês inválido." }
        val card = requireCardAccess(cardId, userId)
        val users = userCache()
        return buildInvoiceResponse(card, month, year, users)
    }

    @Transactional
    fun payInvoice(cardId: UUID, month: Int, year: Int, request: PayInvoiceRequest, userId: UUID): CreditCardInvoiceResponse {
        require(month in 1..12) { "Mês inválido." }
        val card = requireCardAccess(cardId, userId)
        val targetAccountId = request.accountId ?: card.accountId
        val account = accountRepository.findById(targetAccountId) ?: throw AccountNotFoundException("Conta para pagamento não encontrada.")

        val invoice = invoiceRepository.findByCardAndPeriod(cardId, month, year)
            ?: CreditCardInvoiceEntity(creditCardId = cardId, month = month, year = year)

        val currentView = buildInvoiceResponse(card, month, year, userCache())

        if (request.amount > 0) {
            invoice.addPayment(request.amount)
            account.subtractBalance(request.amount)
            accountRepository.save(account)

            val paymentTx = TransactionEntity(
                title = "Pagamento fatura ${card.name} (%02d/%d)".format(month, year),
                amount = request.amount,
                date = request.date,
                responsibleId = userId,
                accountId = account.id,
                type = TransactionType.EXPENSE,
                destinationAccountId = null,
                categoryId = null,
                status = TransactionStatus.COMPLETED
            )
            transactionRepository.save(paymentTx)
        }

        val savedInvoice = invoiceRepository.save(invoice)
        val remainingAfterPayment = (currentView.totalAmount - savedInvoice.paidAmount).coerceAtLeast(0)

        if (request.rollRemainingToNextMonth || request.nextMonthRevolvingInterest != null) {
            val nextYm = YearMonth.of(year, month).plusMonths(1)
            val nextInvoice = invoiceRepository.findByCardAndPeriod(cardId, nextYm.monthValue, nextYm.year)
                ?: CreditCardInvoiceEntity(creditCardId = cardId, month = nextYm.monthValue, year = nextYm.year)
            if (request.rollRemainingToNextMonth) {
                nextInvoice.setCarriedOver(remainingAfterPayment)
            }
            if (request.nextMonthRevolvingInterest != null) {
                nextInvoice.setRevolvingInterest(request.nextMonthRevolvingInterest)
            }
            invoiceRepository.save(nextInvoice)
        }

        return buildInvoiceResponse(card, month, year, userCache())
    }

    @Transactional
    fun updateInvoiceRevolving(cardId: UUID, month: Int, year: Int, request: UpdateInvoiceRevolvingRequest, userId: UUID): CreditCardInvoiceResponse {
        require(month in 1..12) { "Mês inválido." }
        val card = requireCardAccess(cardId, userId)
        val invoice = invoiceRepository.findByCardAndPeriod(cardId, month, year)
            ?: CreditCardInvoiceEntity(creditCardId = cardId, month = month, year = year)
        invoice.setRevolvingInterest(request.revolvingInterestAmount)
        if (request.carriedOverAmount != null) {
            invoice.setCarriedOver(request.carriedOverAmount)
        }
        invoiceRepository.save(invoice)
        return buildInvoiceResponse(card, month, year, userCache())
    }

    fun requireCardAccess(cardId: UUID, userId: UUID): CreditCardEntity {
        val card = creditCardRepository.findById(cardId) ?: throw CreditCardNotFoundException()
        if (card.ownerId == userId) return card
        val gid = card.groupId
        if (gid != null && groupMemberRepository.existsByGroupIdAndUserId(gid, userId)) {
            return card
        }
        throw AccessDeniedException("Você não tem acesso a este cartão de crédito.")
    }

    private fun buildInvoiceResponse(
        card: CreditCardEntity,
        month: Int,
        year: Int,
        users: Map<UUID, UserEntity>
    ): CreditCardInvoiceResponse {
        val txs = transactionRepository.findByCreditCardId(card.id)
            .filter { effectiveInvoicePeriod(card, it) == YearMonth.of(year, month) }
            .sortedByDescending { it.date }

        val invoiceEntity = invoiceRepository.findByCardAndPeriod(card.id, month, year)
        val purchasesAmount = txs.sumOf { if (it.type == TransactionType.INCOME) -it.amount else it.amount }.coerceAtLeast(0)
        val carriedOver = invoiceEntity?.carriedOverAmount ?: 0
        val revolving = invoiceEntity?.revolvingInterestAmount ?: 0
        val paid = invoiceEntity?.paidAmount ?: 0
        val total = purchasesAmount + carriedOver + revolving
        val remaining = (total - paid).coerceAtLeast(0)

        val ym = YearMonth.of(year, month)
        val dueDate = LocalDate.of(year, month, card.dueDay.coerceAtMost(ym.lengthOfMonth()))
        val closingYm = if (card.dueDay < card.closingDay) ym.minusMonths(1) else ym
        val closingDate = LocalDate.of(closingYm.year, closingYm.monthValue, card.closingDay.coerceAtMost(closingYm.lengthOfMonth()))

        val today = LocalDate.now()
        val status = when {
            total > 0 && remaining == 0 -> "PAID"
            paid > 0 && remaining > 0 -> "PARTIALLY_PAID"
            !today.isBefore(closingDate) -> "CLOSED"
            else -> "OPEN"
        }

        val byResponsible = txs.groupBy { it.responsibleId }.mapNotNull { (uid, list) ->
            val u = users[uid] ?: return@mapNotNull null
            val sum = list.sumOf { if (it.type == TransactionType.INCOME) -it.amount else it.amount }
            CreditCardInvoiceResponse.ResponsibleSummary(user = toUserSummary(u), amount = sum)
        }.sortedByDescending { it.amount }

        return CreditCardInvoiceResponse(
            creditCardId = card.id,
            month = month,
            year = year,
            closingDate = closingDate,
            dueDate = dueDate,
            purchasesAmount = purchasesAmount,
            carriedOverAmount = carriedOver,
            revolvingInterestAmount = revolving,
            totalAmount = total,
            paidAmount = paid,
            remainingAmount = remaining,
            status = status,
            transactions = txs.map { TransactionDtoMapper.toResponse(it, users[it.responsibleId]) },
            byResponsible = byResponsible
        )
    }

    private fun toCardResponse(card: CreditCardEntity, users: Map<UUID, UserEntity>): CreditCardResponse {
        val today = LocalDate.now()
        val currentYm = card.defaultInvoiceMonthYear(today)
        val allTxs = transactionRepository.findByCreditCardId(card.id)
        val allInvoices = invoiceRepository.findAllByCreditCardId(card.id)

        // Regra 3:
        // - Compras à vista e parceladas (isInfinite == false) comprometem o limite total (todas as parcelas, atuais e futuras).
        // - Assinaturas (isInfinite == true) comprometem o limite apenas até a fatura atual (currentYm).
        val committedPurchases = allTxs.filter { tx ->
            if (!tx.isInfinite) true
            else effectiveInvoicePeriod(card, tx) <= currentYm
        }.sumOf { if (it.type == TransactionType.INCOME) -it.amount else it.amount }

        val totalRevolvingInterest = allInvoices.sumOf { it.revolvingInterestAmount }
        val totalPaid = allInvoices.sumOf { it.paidAmount }
        val usedLimit = (committedPurchases + totalRevolvingInterest - totalPaid).coerceAtLeast(0)
        val availableLimit = (card.limitAmount - usedLimit).coerceAtLeast(0)

        val currentInvoiceView = buildInvoiceResponse(card, currentYm.monthValue, currentYm.year, users)

        return CreditCardResponse(
            id = card.id,
            ownerId = card.ownerId,
            owner = users[card.ownerId]?.let { toUserSummary(it) },
            accountId = card.accountId,
            name = card.name,
            limitAmount = card.limitAmount,
            usedLimit = usedLimit,
            availableLimit = availableLimit,
            closingDay = card.closingDay,
            dueDay = card.dueDay,
            groupId = card.groupId,
            currentInvoiceMonth = currentYm.monthValue,
            currentInvoiceYear = currentYm.year,
            currentInvoiceAmount = currentInvoiceView.remainingAmount,
            createdAt = card.createdAt,
            updatedAt = card.updatedAt
        )
    }

    private fun effectiveInvoicePeriod(card: CreditCardEntity, tx: TransactionEntity): YearMonth {
        if (tx.invoiceMonth != null && tx.invoiceYear != null) {
            return YearMonth.of(tx.invoiceYear!!, tx.invoiceMonth!!)
        }
        return card.defaultInvoiceMonthYear(tx.date)
    }

    private fun userCache(): Map<UUID, UserEntity> = userRepository.findAll().associateBy { it.id }

    private fun toUserSummary(u: UserEntity) = GroupUserSummary(
        id = u.id,
        firstName = u.firstName.value,
        lastName = u.lastName.value,
        email = u.email.normalized
    )
}
