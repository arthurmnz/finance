package com.finance.application.use_case.account

import com.finance.application.service.GroupService
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.repository.AccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UnlinkAccountFromGroupUseCase(
    private val accountRepository: AccountRepository,
    private val groupService: GroupService
) {

    @Transactional
    fun execute(accountId: UUID, authenticatedUserId: UUID) {
        val account = accountRepository.findById(accountId) ?: throw AccountNotFoundException()
        val groupId = account.groupId ?: return // Já está desvinculada

        if (account.ownerId == authenticatedUserId) {
            // O próprio dono da conta pode desvincular
            account.unlinkFromGroup()
            accountRepository.save(account)
            return
        }

        // Se não for o dono, verifica se quem está tentando é OWNER/ADMIN do grupo atual da conta
        val membership = groupService.findMember(groupId, authenticatedUserId)
        if (membership.canManageMembers()) {
            account.unlinkFromGroup()
            accountRepository.save(account)
        } else {
            throw AccessDeniedException("Apenas o dono da conta ou um administrador do grupo pode desvincular a conta.")
        }
    }
}
