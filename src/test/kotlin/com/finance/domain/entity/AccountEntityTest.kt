package com.finance.domain.entity

import com.finance.domain.enum.AccountType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountEntityTest {

    @Test
    fun `createJoint should create valid joint account with null ownerId and non-null groupId`() {
        val groupId = UUID.randomUUID()
        val account = AccountEntity.createJoint(
            groupId = groupId,
            name = "  Conta Casa  ",
            startBalance = 15000,
            type = AccountType.SAVINGS,
            tax = 12,
            includeInTotal = true
        )

        assertTrue(account.isJoint)
        assertNull(account.ownerId)
        assertEquals(groupId, account.groupId)
        assertEquals("Conta Casa", account.name)
        assertEquals(15000, account.startBalance)
        assertEquals(15000, account.balance)
        assertEquals(AccountType.SAVINGS, account.type)
        assertEquals(12, account.tax)
        assertTrue(account.includeInTotal)
    }

    @Test
    fun `init should enforce joint and individual invariants`() {
        val userId = UUID.randomUUID()
        val groupId = UUID.randomUUID()

        assertThrows<IllegalArgumentException> {
            AccountEntity(
                id = UUID.randomUUID(),
                ownerId = userId,
                name = "Inválida",
                startBalance = 0,
                balance = 0,
                type = AccountType.CHECKING,
                tax = null,
                groupId = groupId,
                isJoint = true
            )
        }

        assertThrows<IllegalArgumentException> {
            AccountEntity(
                id = UUID.randomUUID(),
                ownerId = null,
                name = "Sem Grupo",
                startBalance = 0,
                balance = 0,
                type = AccountType.CHECKING,
                tax = null,
                groupId = null,
                isJoint = true
            )
        }

        assertThrows<IllegalArgumentException> {
            AccountEntity(
                id = UUID.randomUUID(),
                ownerId = null,
                name = "Individual Sem Dono",
                startBalance = 0,
                balance = 0,
                type = AccountType.CHECKING,
                tax = null,
                groupId = null,
                isJoint = false
            )
        }

        assertThrows<IllegalArgumentException> {
            AccountEntity.createJoint(
                groupId = groupId,
                name = "   ",
                startBalance = 0,
                type = AccountType.CHECKING
            )
        }
    }

    @Test
    fun `updateJoint should preserve transactional balance when newBalance is null`() {
        val groupId = UUID.randomUUID()
        val account = AccountEntity.createJoint(
            groupId = groupId,
            name = "Casa",
            startBalance = 10000,
            type = AccountType.CHECKING,
            tax = null,
            includeInTotal = true
        )
        account.subtractBalance(3000) // saldo transacional = 7000 (-3000)

        // Alterar apenas nome e includeInTotal -> preserva balance = 7000
        account.updateJoint(
            newName = "Casa Atualizada",
            newStartBalance = 10000,
            newBalance = null,
            newType = AccountType.WALLET,
            newTax = null,
            newIncludeInTotal = false
        )
        assertEquals("Casa Atualizada", account.name)
        assertEquals(10000, account.startBalance)
        assertEquals(7000, account.balance)
        assertEquals(AccountType.WALLET, account.type)
        assertFalse(account.includeInTotal)

        // Alterar startBalance com newBalance = null -> ajusta pelo delta, preservando -3000 transacional
        account.updateJoint(
            newName = "Casa Atualizada",
            newStartBalance = 15000,
            newBalance = null,
            newType = AccountType.INVESTMENTS,
            newTax = 10,
            newIncludeInTotal = true
        )
        assertEquals(15000, account.startBalance)
        assertEquals(12000, account.balance)
        assertEquals(10, account.tax)

        // Alterar explicitamente newBalance -> sobrescreve balance atual
        account.updateJoint(
            newName = "Casa Final",
            newStartBalance = 15000,
            newBalance = 50000,
            newType = AccountType.CHECKING,
            newTax = null,
            newIncludeInTotal = true
        )
        assertEquals(15000, account.startBalance)
        assertEquals(50000, account.balance)
        assertNull(account.tax)
    }

    @Test
    fun `linkToGroup and unlinkFromGroup should throw IllegalStateException on joint accounts`() {
        val groupId = UUID.randomUUID()
        val account = AccountEntity.createJoint(
            groupId = groupId,
            name = "Conjunta",
            startBalance = 0,
            type = AccountType.CHECKING
        )

        assertThrows<IllegalStateException> {
            account.linkToGroup(UUID.randomUUID())
        }
        assertThrows<IllegalStateException> {
            account.unlinkFromGroup()
        }
    }

    @Test
    fun `name longer than 100 characters should be rejected on create and update`() {
        val longName = "a".repeat(101)
        val groupId = UUID.randomUUID()
        assertThrows<IllegalArgumentException> {
            AccountEntity.createJoint(groupId, longName, 0, AccountType.CHECKING)
        }
        assertThrows<IllegalArgumentException> {
            AccountEntity(ownerId = UUID.randomUUID(), name = longName, startBalance = 0, type = AccountType.CHECKING, tax = null)
        }
        val joint = AccountEntity.createJoint(groupId, "a".repeat(100), 0, AccountType.CHECKING)
        assertThrows<IllegalArgumentException> {
            joint.updateJoint(newName = longName, newStartBalance = 0, newType = AccountType.CHECKING)
        }
        val personal = AccountEntity(ownerId = UUID.randomUUID(), name = "ok", startBalance = 0, type = AccountType.CHECKING, tax = null)
        assertThrows<IllegalArgumentException> {
            personal.update(newName = longName, newStartBalance = 0, newType = AccountType.CHECKING, newTax = null)
        }
    }
}
