package com.finance.application.service

import com.finance.domain.entity.AccountEntity
import com.finance.domain.entity.TransactionEntity
import com.finance.domain.enum.TransactionType
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.exception.CreditCardNotFoundException
import com.finance.domain.exception.UnauthorizedException
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.CreditCardRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.JointAccountMemberRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AccountAccessService(
    private val groupMemberRepository: GroupMemberRepository,
    private val jointAccountMemberRepository: JointAccountMemberRepository
) {
    fun hasAccess(account: AccountEntity, userId: UUID): Boolean {
        if (account.isJoint) {
            val gid = account.groupId ?: return false
            return groupMemberRepository.existsByGroupIdAndUserId(gid, userId) &&
                jointAccountMemberRepository.existsByAccountIdAndUserId(account.id, userId)
        }
        return account.ownerId == userId ||
            (account.groupId != null && groupMemberRepository.existsByGroupIdAndUserId(account.groupId!!, userId))
    }

    fun canManage(account: AccountEntity, userId: UUID): Boolean {
        if (account.isJoint) {
            val gid = account.groupId ?: return false
            val membership = groupMemberRepository.findByGroupIdAndUserId(gid, userId) ?: return false
            return membership.canManageMembers()
        }
        return account.ownerId == userId
    }

    fun requireAccess(
        account: AccountEntity,
        userId: UUID,
        message: String = "Você não tem permissão para acessar esta conta."
    ) {
        if (!hasAccess(account, userId)) {
            throw AccessDeniedException(message)
        }
    }

    /**
     * Valida acesso à conta de origem de uma transferência:
     * - Conta conjunta: exige acesso atual à conta conjunta.
     * - Conta individual: exige que o usuário autenticado seja o titular da conta.
     */
    fun requireTransferSourceAccess(
        account: AccountEntity,
        userId: UUID,
        message: String = "Você só pode realizar transferências a partir de suas próprias contas."
    ) {
        if (account.isJoint) {
            requireAccess(account, userId)
        } else if (account.ownerId != userId) {
            throw AccessDeniedException(message)
        }
    }

    fun requireManage(
        account: AccountEntity,
        userId: UUID,
        message: String = "Você não tem permissão para gerenciar esta conta."
    ) {
        if (!canManage(account, userId)) {
            throw AccessDeniedException(message)
        }
    }

    /**
     * Valida permissão atual para editar, excluir ou alternar status de uma transação.
     * - Cartão: exige autoria da transação, existência do cartão e acesso atual (titular ou membro do grupo).
     * - TRANSFER: exige autoria + titularidade da conta de origem (ou acesso se conjunta) + acesso atual na conta de destino.
     * - EXPENSE/INCOME em conta conjunta: permite qualquer membro com acesso atual à conta.
     * - EXPENSE/INCOME em conta individual: exige autoria + acesso atual à conta.
     */
    fun requireTransactionMutationAccess(
        transaction: TransactionEntity,
        userId: UUID,
        accountRepository: AccountRepository,
        creditCardRepository: CreditCardRepository,
        unauthorizedMessage: String
    ) {
        val cardId = transaction.creditCardId
        if (cardId != null) {
            if (transaction.responsibleId != userId) {
                throw UnauthorizedException(unauthorizedMessage)
            }
            val card = creditCardRepository.findById(cardId) ?: throw CreditCardNotFoundException()
            val hasCardAccess = card.ownerId == userId ||
                (card.groupId != null && groupMemberRepository.existsByGroupIdAndUserId(card.groupId!!, userId))
            if (!hasCardAccess) {
                throw AccessDeniedException("Você não tem acesso a este cartão de crédito.")
            }
            return
        }

        val sourceAccount = accountRepository.findById(transaction.accountId)
            ?: throw AccountNotFoundException("Account not found")

        if (transaction.type == TransactionType.TRANSFER) {
            if (transaction.responsibleId != userId) {
                throw UnauthorizedException(unauthorizedMessage)
            }
            requireTransferSourceAccess(sourceAccount, userId)
            val destinationAccount = transaction.destinationAccountId?.let { accountRepository.findById(it) }
                ?: throw AccountNotFoundException("Destination account not found")
            requireAccess(destinationAccount, userId)
            return
        }

        if (sourceAccount.isJoint) {
            requireAccess(sourceAccount, userId)
        } else {
            if (transaction.responsibleId != userId) {
                throw UnauthorizedException(unauthorizedMessage)
            }
            requireAccess(sourceAccount, userId)
        }
    }
}
