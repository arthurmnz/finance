package com.finance.presentation.controller

import com.finance.application.dto.credit_card.CreditCardInvoiceResponse
import com.finance.application.dto.credit_card.CreditCardResponse
import com.finance.application.dto.credit_card.CreateCreditCardRequest
import com.finance.application.dto.credit_card.PayInvoiceRequest
import com.finance.application.dto.credit_card.UpdateCreditCardRequest
import com.finance.application.dto.credit_card.UpdateInvoiceRevolvingRequest
import com.finance.application.service.CreditCardService
import com.finance.domain.entity.CreditCardGroupRequestEntity
import com.finance.domain.entity.UserEntity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/credit-cards")
@Tag(name = "Credit Cards", description = "Endpoints para gerenciamento de cartões de crédito e faturas")
class CreditCardController(
    private val creditCardService: CreditCardService
) {

    @PostMapping
    @Operation(summary = "Criar cartão de crédito")
    fun create(
        @RequestBody request: CreateCreditCardRequest,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<CreditCardResponse> {
        val response = creditCardService.create(request, authenticatedUser.id)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar cartão de crédito")
    fun update(
        @PathVariable id: UUID,
        @RequestBody request: UpdateCreditCardRequest,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<CreditCardResponse> {
        val response = creditCardService.update(id, request, authenticatedUser.id)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir cartão de crédito")
    fun delete(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<Void> {
        creditCardService.delete(id, authenticatedUser.id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping
    @Operation(summary = "Listar cartões acessíveis (próprios e de grupos)")
    fun listAccessible(
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<List<CreditCardResponse>> {
        return ResponseEntity.ok(creditCardService.listAccessibleCards(authenticatedUser.id))
    }

    @PostMapping("/{id}/group/{groupId}")
    @Operation(summary = "Vincular cartão a um grupo")
    fun linkToGroup(
        @PathVariable id: UUID,
        @PathVariable groupId: UUID,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<Any> {
        val response = creditCardService.linkToGroup(id, groupId, authenticatedUser.id)
        if (response is CreditCardGroupRequestEntity) {
            return ResponseEntity.accepted().body(
                mapOf("message" to "Solicitação de vínculo pendente enviada.", "requestId" to response.id)
            )
        }
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}/group")
    @Operation(summary = "Desvincular cartão do grupo")
    fun unlinkFromGroup(
        @PathVariable id: UUID,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<Void> {
        creditCardService.unlinkFromGroup(id, authenticatedUser.id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{id}/invoice")
    @Operation(summary = "Obter fatura de um mês/ano")
    fun getInvoice(
        @PathVariable id: UUID,
        @RequestParam month: Int,
        @RequestParam year: Int,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<CreditCardInvoiceResponse> {
        return ResponseEntity.ok(creditCardService.getInvoice(id, month, year, authenticatedUser.id))
    }

    @PostMapping("/{id}/invoice/pay")
    @Operation(summary = "Pagar fatura manualmente (total ou parcial) e opcionalmente rolar saldo para próxima fatura")
    fun payInvoice(
        @PathVariable id: UUID,
        @RequestParam month: Int,
        @RequestParam year: Int,
        @RequestBody request: PayInvoiceRequest,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<CreditCardInvoiceResponse> {
        return ResponseEntity.ok(creditCardService.payInvoice(id, month, year, request, authenticatedUser.id))
    }

    @PutMapping("/{id}/invoice/revolving")
    @Operation(summary = "Atualizar valor de juros/encargos do rotativo na fatura")
    fun updateRevolving(
        @PathVariable id: UUID,
        @RequestParam month: Int,
        @RequestParam year: Int,
        @RequestBody request: UpdateInvoiceRevolvingRequest,
        @AuthenticationPrincipal authenticatedUser: UserEntity
    ): ResponseEntity<CreditCardInvoiceResponse> {
        return ResponseEntity.ok(creditCardService.updateInvoiceRevolving(id, month, year, request, authenticatedUser.id))
    }
}
