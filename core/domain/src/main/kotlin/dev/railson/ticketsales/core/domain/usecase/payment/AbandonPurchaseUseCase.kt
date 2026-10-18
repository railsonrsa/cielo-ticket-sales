package dev.railson.ticketsales.core.domain.usecase.payment

import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.repository.PurchaseRepository

/**
 * "Desistir desta compra": a saída de emergência da órfã (ADR 0001, item 7). Sem ela, a Cielo fora do ar travaria
 * a maquininha para sempre. Só vale depois de ao menos uma verificação inconclusiva, e nunca durante uma.
 * Uma aprovação que chegue depois cai na regra da aprovação atrasada.
 */
class AbandonPurchaseUseCase(
    private val purchaseRepository: PurchaseRepository,
    private val reconcilePurchase: ReconcilePurchaseUseCase,
) {

    /** Devolve se a Purchase foi abandonada. */
    suspend operator fun invoke(purchaseId: String): Boolean {
        if (reconcilePurchase.isRunning.value) return false
        val purchase = purchaseRepository.find(purchaseId)
        if (purchase == null || !purchase.canBeAbandoned) return false
        purchaseRepository.fail(purchase.id, FailureReason.Abandoned)
        return true
    }
}
