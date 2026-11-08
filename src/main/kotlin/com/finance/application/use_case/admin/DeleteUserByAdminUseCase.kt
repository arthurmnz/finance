package com.finance.application.use_case.admin

import com.finance.domain.enum.GroupRole
import com.finance.domain.exception.UserNotFoundException
import com.finance.domain.repository.UserRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DeleteUserByAdminUseCase(
    private val userRepository: UserRepository,
    private val entityManager: EntityManager,
) {
    @Transactional
    fun execute(authenticatedAdminId: UUID, targetUserId: UUID) {
        if (authenticatedAdminId == targetUserId) {
            throw IllegalArgumentException("Você não pode excluir sua própria conta de administrador.")
        }

        userRepository.findById(targetUserId)
            ?: throw UserNotFoundException()

        // 1. Identificar entidades diretamente pertencentes ao usuário ou grupos de que é dono
        val userAccountIds = entityManager.createQuery(
            "SELECT a.id FROM AccountJpaEntity a WHERE a.ownerId = :userId",
            UUID::class.java
        ).setParameter("userId", targetUserId).resultList

        val userCardIds = entityManager.createQuery(
            "SELECT c.id FROM CreditCardJpaEntity c WHERE c.ownerId = :userId",
            UUID::class.java
        ).setParameter("userId", targetUserId).resultList

        val userCategoryIds = entityManager.createQuery(
            "SELECT cat.id FROM CategoryJpaEntity cat WHERE cat.userId = :userId",
            UUID::class.java
        ).setParameter("userId", targetUserId).resultList

        val ownedGroupIds = entityManager.createQuery(
            "SELECT gm.groupId FROM GroupMemberJpaEntity gm WHERE gm.userId = :userId AND gm.role = :role",
            UUID::class.java
        ).setParameter("userId", targetUserId).setParameter("role", GroupRole.OWNER).resultList

        val ownedGroupCategoryIds = if (ownedGroupIds.isNotEmpty()) {
            entityManager.createQuery(
                "SELECT cat.id FROM CategoryJpaEntity cat WHERE cat.groupId IN :groupIds",
                UUID::class.java
            ).setParameter("groupIds", ownedGroupIds).resultList
        } else {
            emptyList()
        }

        // 3. Apagar debt_settlements
        if (ownedGroupIds.isNotEmpty()) {
            entityManager.createQuery(
                "DELETE FROM DebtSettlementJpaEntity ds WHERE ds.debtorId = :userId OR ds.creditorId = :userId OR ds.groupId IN :groupIds"
            ).setParameter("userId", targetUserId).setParameter("groupIds", ownedGroupIds).executeUpdate()
        } else {
            entityManager.createQuery(
                "DELETE FROM DebtSettlementJpaEntity ds WHERE ds.debtorId = :userId OR ds.creditorId = :userId"
            ).setParameter("userId", targetUserId).executeUpdate()
        }

        // 4. Apagar transactions vinculadas ao usuário
        entityManager.createQuery(
            "DELETE FROM TransactionJpaEntity t WHERE t.responsibleId = :userId"
        ).setParameter("userId", targetUserId).executeUpdate()

        if (userAccountIds.isNotEmpty()) {
            entityManager.createQuery(
                "DELETE FROM TransactionJpaEntity t WHERE t.accountId IN :accIds OR t.destinationAccountId IN :accIds"
            ).setParameter("accIds", userAccountIds).executeUpdate()
        }

        if (userCardIds.isNotEmpty()) {
            entityManager.createQuery(
                "DELETE FROM TransactionJpaEntity t WHERE t.creditCardId IN :cardIds"
            ).setParameter("cardIds", userCardIds).executeUpdate()
        }

        val allCategoryIds = (userCategoryIds + ownedGroupCategoryIds).distinct()
        if (allCategoryIds.isNotEmpty()) {
            entityManager.createQuery(
                "DELETE FROM TransactionJpaEntity t WHERE t.categoryId IN :catIds"
            ).setParameter("catIds", allCategoryIds).executeUpdate()
        }

        // 5. Apagar credit_card_group_requests
        entityManager.createQuery(
            "DELETE FROM CreditCardGroupRequestJpaEntity r WHERE r.requesterId = :userId"
        ).setParameter("userId", targetUserId).executeUpdate()

        if (userCardIds.isNotEmpty()) {
            entityManager.createQuery(
                "DELETE FROM CreditCardGroupRequestJpaEntity r WHERE r.creditCardId IN :cardIds"
            ).setParameter("cardIds", userCardIds).executeUpdate()
        }

        if (ownedGroupIds.isNotEmpty()) {
            entityManager.createQuery(
                "DELETE FROM CreditCardGroupRequestJpaEntity r WHERE r.groupId IN :groupIds"
            ).setParameter("groupIds", ownedGroupIds).executeUpdate()
        }

        // 6. Apagar credit_card_invoices
        if (userCardIds.isNotEmpty()) {
            entityManager.createQuery(
                "DELETE FROM CreditCardInvoiceJpaEntity i WHERE i.creditCardId IN :cardIds"
            ).setParameter("cardIds", userCardIds).executeUpdate()
        }

        // 7. Apagar credit_cards
        entityManager.createQuery(
            "DELETE FROM CreditCardJpaEntity c WHERE c.ownerId = :userId"
        ).setParameter("userId", targetUserId).executeUpdate()

        // 8. Apagar account_group_requests
        entityManager.createQuery(
            "DELETE FROM AccountGroupRequestJpaEntity r WHERE r.requesterId = :userId"
        ).setParameter("userId", targetUserId).executeUpdate()

        if (userAccountIds.isNotEmpty()) {
            entityManager.createQuery(
                "DELETE FROM AccountGroupRequestJpaEntity r WHERE r.accountId IN :accIds"
            ).setParameter("accIds", userAccountIds).executeUpdate()
        }

        if (ownedGroupIds.isNotEmpty()) {
            entityManager.createQuery(
                "DELETE FROM AccountGroupRequestJpaEntity r WHERE r.groupId IN :groupIds"
            ).setParameter("groupIds", ownedGroupIds).executeUpdate()
        }

        // 9. Apagar accounts
        entityManager.createQuery(
            "DELETE FROM AccountJpaEntity a WHERE a.ownerId = :userId"
        ).setParameter("userId", targetUserId).executeUpdate()

        // 10. Apagar group_invitations
        entityManager.createQuery(
            "DELETE FROM GroupInvitationJpaEntity i WHERE i.inviterId = :userId OR i.inviteeId = :userId"
        ).setParameter("userId", targetUserId).executeUpdate()

        if (ownedGroupIds.isNotEmpty()) {
            entityManager.createQuery(
                "DELETE FROM GroupInvitationJpaEntity i WHERE i.groupId IN :groupIds"
            ).setParameter("groupIds", ownedGroupIds).executeUpdate()
        }

        // 11. Apagar categories
        entityManager.createQuery(
            "DELETE FROM CategoryJpaEntity c WHERE c.userId = :userId"
        ).setParameter("userId", targetUserId).executeUpdate()

        if (ownedGroupIds.isNotEmpty()) {
            entityManager.createQuery(
                "DELETE FROM CategoryJpaEntity c WHERE c.groupId IN :groupIds"
            ).setParameter("groupIds", ownedGroupIds).executeUpdate()
        }

        // 12. Apagar group_members
        entityManager.createQuery(
            "DELETE FROM GroupMemberJpaEntity gm WHERE gm.userId = :userId"
        ).setParameter("userId", targetUserId).executeUpdate()

        if (ownedGroupIds.isNotEmpty()) {
            entityManager.createQuery(
                "DELETE FROM GroupMemberJpaEntity gm WHERE gm.groupId IN :groupIds"
            ).setParameter("groupIds", ownedGroupIds).executeUpdate()
        }

        // 13. Apagar groups pertencentes ao usuário (OWNER)
        if (ownedGroupIds.isNotEmpty()) {
            entityManager.createQuery(
                "DELETE FROM GroupJpaEntity g WHERE g.id IN :groupIds"
            ).setParameter("groupIds", ownedGroupIds).executeUpdate()
        }

        // 14. Apagar friendships
        entityManager.createQuery(
            "DELETE FROM FriendshipJpaEntity f WHERE f.requesterId = :userId OR f.addresseeId = :userId"
        ).setParameter("userId", targetUserId).executeUpdate()

        // 15. Apagar password_recovery_tokens
        entityManager.createQuery(
            "DELETE FROM PasswordRecoveryTokenJpaEntity p WHERE p.userId = :userId"
        ).setParameter("userId", targetUserId).executeUpdate()

        // 16. Apagar refresh_tokens
        entityManager.createQuery(
            "DELETE FROM RefreshTokenJpaEntity r WHERE r.userId = :userId"
        ).setParameter("userId", targetUserId).executeUpdate()

        // 17. Por fim, apagar o usuário
        userRepository.deleteById(targetUserId)
    }
}
