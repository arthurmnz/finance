package com.finance.domain.entity

import com.finance.domain.enum.AccountType
import com.finance.domain.exception.InvalidTransactionException
import java.time.LocalDateTime
import java.util.UUID

class AccountEntity(
    val id: UUID,
    val ownerId: UUID?,
    name: String,
    startBalance: Int,
    balance: Int,
    type: AccountType,
    tax: Int?,
    groupId: UUID?,
    includeInTotal: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now(),
    val isJoint: Boolean = false
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

    var includeInTotal: Boolean = includeInTotal
        private set

    var updatedAt: LocalDateTime = updatedAt
        private set

    init {
        require(name.isNotBlank()) { "Nome da conta não pode ser vazio." }
        require(name.trim().length <= 100) { "O nome da conta deve ter no máximo 100 caracteres." }
        if (isJoint) {
            require(ownerId == null) { "Conta conjunta não pode possuir um dono individual." }
            require(groupId != null) { "Conta conjunta deve estar vinculada a um grupo." }
        } else {
            require(ownerId != null) { "Conta individual deve possuir um dono." }
        }
    }

    constructor(
        ownerId: UUID,
        name: String,
        startBalance: Int,
        type: AccountType,
        tax: Int?,
        includeInTotal: Boolean = true
    ) : this(
        id = UUID.randomUUID(),
        ownerId = ownerId,
        name = name.trim(),
        startBalance = startBalance,
        balance = startBalance,
        type = type,
        tax = validateTax(type, tax),
        groupId = null,
        includeInTotal = includeInTotal,
        isJoint = false
    ) {
        require(name.isNotBlank()) { "Nome da conta não pode ser vazio." }
    }

    fun update(
        newName: String,
        newStartBalance: Int,
        newType: AccountType,
        newTax: Int?,
        newIncludeInTotal: Boolean = this.includeInTotal
    ) {
        require(newName.isNotBlank()) { "Nome da conta não pode ser vazio." }
        require(newName.trim().length <= 100) { "O nome da conta deve ter no máximo 100 caracteres." }
        
        // Se o saldo inicial mudar, ajustamos o saldo atual.
        if (this.startBalance != newStartBalance) {
            this.balance = newStartBalance
            this.startBalance = newStartBalance
        }

        this.name = newName.trim()
        this.type = newType
        this.tax = validateTax(newType, newTax)
        this.includeInTotal = newIncludeInTotal
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * Atualiza dados de uma conta conjunta preservando o saldo transacional quando
     * [newBalance] não é informado explicitamente.
     */
    fun updateJoint(
        newName: String,
        newStartBalance: Int,
        newBalance: Int? = null,
        newType: AccountType,
        newTax: Int? = null,
        newIncludeInTotal: Boolean = this.includeInTotal
    ) {
        check(this.isJoint) { "Esta operação só é permitida para contas conjuntas." }
        require(newName.isNotBlank()) { "Nome da conta não pode ser vazio." }
        require(newName.trim().length <= 100) { "O nome da conta deve ter no máximo 100 caracteres." }

        if (newBalance != null) {
            this.balance = newBalance
            this.startBalance = newStartBalance
        } else if (this.startBalance != newStartBalance) {
            this.balance += (newStartBalance - this.startBalance)
            this.startBalance = newStartBalance
        }

        this.name = newName.trim()
        this.type = newType
        this.tax = validateTax(newType, newTax)
        this.includeInTotal = newIncludeInTotal
        this.updatedAt = LocalDateTime.now()
    }

    fun linkToGroup(newGroupId: UUID) {
        check(!this.isJoint) { "Conta conjunta não pode ser vinculada manualmente a outro grupo." }
        check(this.groupId == null) { "Conta já está vinculada a um grupo." }
        this.groupId = newGroupId
        this.updatedAt = LocalDateTime.now()
    }

    fun unlinkFromGroup() {
        check(!this.isJoint) { "Conta conjunta não pode ser desvinculada do grupo." }
        this.groupId = null
        this.updatedAt = LocalDateTime.now()
    }

    fun addBalance(amount: Int) {
        val nextBalance = this.balance.toLong() + amount.toLong()
        if (nextBalance > Int.MAX_VALUE || nextBalance < Int.MIN_VALUE) {
            throw InvalidTransactionException("O saldo resultante da conta excede o limite suportado.")
        }
        this.balance = nextBalance.toInt()
        this.updatedAt = LocalDateTime.now()
    }

    fun subtractBalance(amount: Int) {
        val nextBalance = this.balance.toLong() - amount.toLong()
        if (nextBalance > Int.MAX_VALUE || nextBalance < Int.MIN_VALUE) {
            throw InvalidTransactionException("O saldo resultante da conta excede o limite suportado.")
        }
        this.balance = nextBalance.toInt()
        this.updatedAt = LocalDateTime.now()
    }

    companion object {
        fun createJoint(
            groupId: UUID,
            name: String,
            startBalance: Int,
            type: AccountType,
            tax: Int? = null,
            includeInTotal: Boolean = true
        ): AccountEntity {
            require(name.isNotBlank()) { "Nome da conta não pode ser vazio." }
            return AccountEntity(
                id = UUID.randomUUID(),
                ownerId = null,
                name = name.trim(),
                startBalance = startBalance,
                balance = startBalance,
                type = type,
                tax = validateTax(type, tax),
                groupId = groupId,
                includeInTotal = includeInTotal,
                isJoint = true
            )
        }

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
