package com.finance.infraestructure.persistence.mapper

import com.finance.domain.entity.CreditCardEntity
import com.finance.domain.entity.CreditCardGroupRequestEntity
import com.finance.domain.entity.CreditCardInvoiceEntity
import com.finance.infraestructure.persistence.entity.CreditCardGroupRequestJpaEntity
import com.finance.infraestructure.persistence.entity.CreditCardInvoiceJpaEntity
import com.finance.infraestructure.persistence.entity.CreditCardJpaEntity
import org.springframework.stereotype.Component

@Component
class CreditCardMapper {
    fun toDomain(jpa: CreditCardJpaEntity) = CreditCardEntity(
        id = jpa.id,
        ownerId = jpa.ownerId,
        accountId = jpa.accountId,
        name = jpa.name,
        limitAmount = jpa.limitAmount,
        closingDay = jpa.closingDay,
        dueDay = jpa.dueDay,
        groupId = jpa.groupId,
        createdAt = jpa.createdAt,
        updatedAt = jpa.updatedAt
    )

    fun toJpaEntity(d: CreditCardEntity) = CreditCardJpaEntity(
        id = d.id,
        ownerId = d.ownerId,
        accountId = d.accountId,
        name = d.name,
        limitAmount = d.limitAmount,
        closingDay = d.closingDay,
        dueDay = d.dueDay,
        groupId = d.groupId,
        createdAt = d.createdAt,
        updatedAt = d.updatedAt
    )

    fun toInvoiceDomain(jpa: CreditCardInvoiceJpaEntity) = CreditCardInvoiceEntity(
        id = jpa.id,
        creditCardId = jpa.creditCardId,
        month = jpa.month,
        year = jpa.year,
        paidAmount = jpa.paidAmount,
        carriedOverAmount = jpa.carriedOverAmount,
        revolvingInterestAmount = jpa.revolvingInterestAmount,
        createdAt = jpa.createdAt,
        updatedAt = jpa.updatedAt
    )

    fun toInvoiceJpa(d: CreditCardInvoiceEntity) = CreditCardInvoiceJpaEntity(
        id = d.id,
        creditCardId = d.creditCardId,
        month = d.month,
        year = d.year,
        paidAmount = d.paidAmount,
        carriedOverAmount = d.carriedOverAmount,
        revolvingInterestAmount = d.revolvingInterestAmount,
        createdAt = d.createdAt,
        updatedAt = d.updatedAt
    )

    fun toRequestDomain(jpa: CreditCardGroupRequestJpaEntity) = CreditCardGroupRequestEntity(
        id = jpa.id,
        creditCardId = jpa.creditCardId,
        groupId = jpa.groupId,
        requesterId = jpa.requesterId,
        status = jpa.status,
        createdAt = jpa.createdAt,
        updatedAt = jpa.updatedAt
    )

    fun toRequestJpa(d: CreditCardGroupRequestEntity) = CreditCardGroupRequestJpaEntity(
        id = d.id,
        creditCardId = d.creditCardId,
        groupId = d.groupId,
        requesterId = d.requesterId,
        status = d.status,
        createdAt = d.createdAt,
        updatedAt = d.updatedAt
    )
}
