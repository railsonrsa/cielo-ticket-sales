package dev.railson.ticketsales.core.domain.usecase.payment

import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.model.PurchaseStatus
import dev.railson.ticketsales.core.domain.repository.PurchaseRepository
import dev.railson.ticketsales.core.domain.terminal.PaymentOutcome

/** Onde a resposta da maquininha entra, venha da Cielo ou da fake (ADR 0002). Lê e grava só o que está em disco. */
class HandlePaymentOutcomeUseCase(private val purchaseRepository: PurchaseRepository) {

    suspend operator fun invoke(outcome: PaymentOutcome) {
        when (outcome) {
            is PaymentOutcome.Paid -> handlePaid(outcome)
            // A falha da Cielo não traz `reference`: só pode ser da única Purchase AwaitingPayment.
            is PaymentOutcome.Failed -> purchaseRepository.findAwaitingPayment()
                ?.let { purchaseRepository.fail(it.id, outcome.reason) }
            PaymentOutcome.Unreadable -> Unit
        }
    }

    private suspend fun handlePaid(outcome: PaymentOutcome.Paid) {
        val purchase = purchaseRepository.find(outcome.reference) ?: return
        // Só transiciona a partir de AwaitingPayment. A promoção atrasada Failed → Approved é o #19.
        if (purchase.status != PurchaseStatus.AwaitingPayment) return
        // Aprova só se a Cielo autorizou o total exato. Senão, o dinheiro que saiu fica marcado para estorno.
        if (outcome.cieloOrder.authorizedAmount == purchase.total) {
            purchaseRepository.approve(purchase.id, outcome.cieloOrder)
        } else {
            purchaseRepository.fail(purchase.id, FailureReason.AmountMismatch, outcome.cieloOrder, pendingRefund = true)
        }
    }
}
