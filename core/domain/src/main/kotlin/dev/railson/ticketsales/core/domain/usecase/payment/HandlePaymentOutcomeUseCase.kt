package dev.railson.ticketsales.core.domain.usecase.payment

import dev.railson.ticketsales.core.domain.model.CieloOrder
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.model.Purchase
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

    /**
     * Só transiciona a partir de `AwaitingPayment`, com uma exceção: o dinheiro que sai depois de o app ter dado a
     * Purchase como `Failed` (ADR 0001, item 6). Um `Approved` ignora callbacks repetidos.
     */
    private suspend fun handlePaid(outcome: PaymentOutcome.Paid) {
        val purchase = purchaseRepository.find(outcome.reference) ?: return
        when (val status = purchase.status) {
            PurchaseStatus.AwaitingPayment -> settle(purchase, outcome.cieloOrder, late = false)
            is PurchaseStatus.Failed ->
                if (purchase.pendingRefund) addToRefund(purchase, status.reason, outcome.cieloOrder) else settle(purchase, outcome.cieloOrder, late = true)
            PurchaseStatus.Approved -> Unit
        }
    }

    /** Aprova só se a Cielo autorizou o total exato. Senão, o dinheiro que saiu fica marcado para estorno. */
    private suspend fun settle(purchase: Purchase, cieloOrder: CieloOrder, late: Boolean) {
        if (cieloOrder.authorizedAmount == purchase.total) {
            purchaseRepository.approveOrRefund(purchase.id, cieloOrder, late = late)
        } else {
            purchaseRepository.fail(purchase.id, FailureReason.AmountMismatch, cieloOrder, pendingRefund = true)
        }
    }

    /** A Purchase já vai para estorno: outra Cielo Order paga entra junto, e uma repetida não muda nada. */
    private suspend fun addToRefund(purchase: Purchase, reason: FailureReason, cieloOrder: CieloOrder) {
        val known = listOfNotNull(purchase.cieloOrder?.orderId) + purchase.extraOrderIds
        if (cieloOrder.orderId in known) return
        purchaseRepository.fail(
            purchaseId = purchase.id,
            reason = reason,
            cieloOrder = purchase.cieloOrder,
            pendingRefund = true,
            extraOrderIds = purchase.extraOrderIds + cieloOrder.orderId,
            needsAcknowledgment = purchase.needsAcknowledgment,
        )
    }
}
