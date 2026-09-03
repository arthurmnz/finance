package com.finance.application.use_case.user

import com.finance.application.service.UserService
import com.finance.domain.enum.GroupRole
import com.finance.domain.repository.GroupInvitationRepository
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
import com.finance.domain.repository.AccountRepository
import com.finance.domain.repository.AccountGroupRequestRepository
import com.finance.domain.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DeleteUserUseCase(
    private val userRepository: UserRepository,
    private val userService: UserService,
    private val groupMemberRepository: GroupMemberRepository,
    private val groupRepository: GroupRepository,
    private val groupInvitationRepository: GroupInvitationRepository,
    private val accountRepository: AccountRepository,
    private val accountGroupRequestRepository: AccountGroupRequestRepository,
) {

    @Transactional
    fun execute(userId: UUID, authenticatedUserId: UUID? = null) {
        if (authenticatedUserId != null) {
            userService.validateUserOwnership(authenticatedUserId, userId)
        }

        // Ensures user exists before deletion or throws UserNotFoundException
        userService.findById(userId)

        // Trata os grupos onde o usuário é OWNER antes de deletar
        handleGroupOwnership(userId)

        // Deleta os convites e requisições pendentes de contas
        accountGroupRequestRepository.deleteAllByAccountId(userId) // TODO: Isso precisa ser pelas contas do usuário, não dele direto
        // Para simplificar, deleto por query separada na função abaixo

        handleAccounts(userId)

        userRepository.deleteById(userId)
    }

    private fun handleAccounts(userId: UUID) {
        val userAccounts = accountRepository.findAllByOwnerId(userId)
        userAccounts.forEach { account ->
            accountGroupRequestRepository.deleteAllByAccountId(account.id)
        }
        accountRepository.deleteAllByOwnerId(userId)
    }

    private fun handleGroupOwnership(userId: UUID) {
        val memberships = groupMemberRepository.findAllByUserId(userId)

        memberships.filter { it.isOwner() }.forEach { ownerMembership ->
            val groupId = ownerMembership.groupId

            // Busca o ADMIN mais antigo para assumir o grupo
            val nextOwner = groupMemberRepository.findOldestAdminByGroupId(groupId)
                ?: groupMemberRepository.findOldestMemberByGroupIdExcludingOwner(groupId)

            if (nextOwner != null) {
                nextOwner.promoteToOwner()
                groupMemberRepository.save(nextOwner)
            } else {
                // Nenhum outro membro: exclui o grupo junto com convites pendentes
                groupInvitationRepository.deleteAllByGroupId(groupId)
                groupMemberRepository.deleteAllByGroupId(groupId)
                groupRepository.deleteById(groupId)
            }
        }

        // Remove o usuário de todos os grupos em que era membro ou admin
        groupMemberRepository.deleteAllByUserId(userId)
    }
}
