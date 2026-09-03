package com.finance.domain.entity

import com.finance.domain.enum.AccountType
import java.time.LocalDateTime
import java.util.UUID

class AccountEntity(
    val id: UUID,
    val ownerId: UUID,
    name: String,
    startBalance: Int,
    balance: Int,
    type: AccountType,
    tax: Int?,
    groupId: UUID?,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now()
) {
    var name: String = name
        private set

    var startBalance: Int = startBalance
        private set

    var balance: Int = balance
        private set

    var type: AccountType = type
        private set

    var tax: Int? = tax
        private set

    var groupId: UUID? = groupId
        private set

    var updatedAt: LocalDateTime = updatedAt
        private set

    constructor(
        ownerId: UUID,
        name: String,
        startBalance: Int,
        type: AccountType,
        tax: Int?
    ) : this(
        id = UUID.randomUUID(),
        ownerId = ownerId,
        name = name.trim(),
        startBalance = startBalance,
        balance = startBalance,
        type = type,
        tax = validateTax(type, tax),
        groupId = null
    ) {
        require(name.isNotBlank()) { "Nome da conta não pode ser vazio." }
    }

    fun update(newName: String, newStartBalance: Int, newType: AccountType, newTax: Int?) {
        require(newName.isNotBlank()) { "Nome da conta não pode ser vazio." }
        
        // Se o saldo inicial mudar, ajustamos o saldo atual.
        if (this.startBalance != newStartBalance) {
            this.balance = newStartBalance
            this.startBalance = newStartBalance
        }

        this.name = newName.trim()
        this.type = newType
        this.tax = validateTax(newType, newTax)
        this.updatedAt = LocalDateTime.now()
    }

    fun linkToGroup(newGroupId: UUID) {
        check(this.groupId == null) { "Conta já está vinculada a um grupo." }
        this.groupId = newGroupId
        this.updatedAt = LocalDateTime.now()
    }

    fun unlinkFromGroup() {
        this.groupId = null
        this.updatedAt = LocalDateTime.now()
    }

    fun addBalance(amount: Int) {
        this.balance += amount
        this.updatedAt = LocalDateTime.now()
    }

    fun subtractBalance(amount: Int) {
        this.balance -= amount
        this.updatedAt = LocalDateTime.now()
    }

    companion object {
        private fun validateTax(type: AccountType, tax: Int?): Int? {
            if (type == AccountType.INVESTMENTS || type == AccountType.SAVINGS) {
                requireNotNull(tax) { "Contas de investimento ou poupança exigem uma taxa definida." }
                return tax
            }
            return null // Garante que WALLET e CHECKING não tenham taxa
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AccountEntity) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
