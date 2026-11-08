package com.finance.application.use_case.admin

import com.finance.application.dto.admin.AdminUserResponse
import com.finance.infraestructure.persistence.entity.UserJpaEntity
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ListAdminUsersUseCase(
    private val entityManager: EntityManager
) {
    @Transactional(readOnly = true)
    fun execute(): List<AdminUserResponse> {
        val users = entityManager.createQuery(
            "SELECT u FROM UserJpaEntity u ORDER BY u.createdAt DESC",
            UserJpaEntity::class.java
        ).resultList

        if (users.isEmpty()) {
            return emptyList()
        }

        val accountsCountMap = entityManager.createQuery(
            "SELECT a.ownerId, COUNT(a) FROM AccountJpaEntity a GROUP BY a.ownerId",
            Array<Any>::class.java
        ).resultList.associate { (it[0] as UUID) to (it[1] as Long) }

        val cardsCountMap = entityManager.createQuery(
            "SELECT c.ownerId, COUNT(c) FROM CreditCardJpaEntity c GROUP BY c.ownerId",
            Array<Any>::class.java
        ).resultList.associate { (it[0] as UUID) to (it[1] as Long) }

        val groupsCountMap = entityManager.createQuery(
            "SELECT gm.userId, COUNT(gm) FROM GroupMemberJpaEntity gm GROUP BY gm.userId",
            Array<Any>::class.java
        ).resultList.associate { (it[0] as UUID) to (it[1] as Long) }

        val transactionsCountMap = entityManager.createQuery(
            "SELECT t.responsibleId, COUNT(t) FROM TransactionJpaEntity t GROUP BY t.responsibleId",
            Array<Any>::class.java
        ).resultList.associate { (it[0] as UUID) to (it[1] as Long) }

        return users.map { u ->
            AdminUserResponse(
                id = u.id,
                firstName = u.firstName,
                lastName = u.lastName,
                email = u.email,
                role = u.role,
                isActive = u.isActive,
                createdAt = u.createdAt,
                updatedAt = u.updatedAt,
                accountsCount = accountsCountMap[u.id] ?: 0L,
                creditCardsCount = cardsCountMap[u.id] ?: 0L,
                groupsCount = groupsCountMap[u.id] ?: 0L,
                transactionsCount = transactionsCountMap[u.id] ?: 0L,
            )
        }
    }
}
