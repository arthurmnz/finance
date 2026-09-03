package com.finance.application.use_case.account

import com.finance.application.dto.account.AccountResponse
import com.finance.application.mapper.AccountDtoMapper
import com.finance.application.service.GroupService
import com.finance.domain.entity.AccountGroupRequestEntity
import com.finance.domain.enum.AccountGroupRequestStatus
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.AccountAlreadyLinkedException
import com.finance.domain.exception.AccountGroupRequestAlreadyExistsException
import com.finance.domain.exception.AccountNotFoundException
import com.finance.domain.repository.AccountGroupRequestRepository
import com.finance.domain.repository.AccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class LinkAccountToGroupUseCase(
    private val accountRepository: AccountRepository,
    private val groupService: GroupService,
    private val accountGroupRequestRepository: AccountGroupRequestRepository,
    private val accountDtoMapper: AccountDtoMapper
) {

    @Transactional
    fun execute(accountId: UUID, groupId: UUID, authenticatedUserId: UUID): Any {
        val account = accountRepository.findById(accountId) ?: throw AccountNotFoundException()
        
        if (account.ownerId != authenticatedUserId) {
            throw AccessDeniedException("Você só pode vincular suas próprias contas a um grupo.")
        }

        if (account.groupId != null) {
            throw AccountAlreadyLinkedException()
        }

        val existingRequest = accountGroupRequestRepository.findByAccountIdAndStatus(accountId, AccountGroupRequestStatus.PENDING)
        if (existingRequest != null) {
            throw AccountGroupRequestAlreadyExistsException()
        }

        val membership = groupService.findMember(groupId, authenticatedUserId)

        if (membership.canManageMembers()) {
            // É OWNER ou ADMIN: o vínculo é imediato
            account.linkToGroup(groupId)
            val saved = accountRepository.save(account)
            return accountDtoMapper.toAccountResponse(saved)
        } else {
            // É MEMBER: cria solicitação
            val request = AccountGroupRequestEntity(
                accountId = account.id,
                groupId = groupId,
                requesterId = authenticatedUserId
            )
            return accountGroupRequestRepository.save(request) // Retorna request na controller para formatar DTO, se necessário.
            // Para simplificar e retornar o correto de acordo com a view, farei retornar um status ou criar um wrapper.
            // Vou retornar uma string informando o estado. (Veja ajuste na controller)
        }
    }
}
