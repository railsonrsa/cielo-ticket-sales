package dev.railson.ticketsales.core.domain.terminal

import dev.railson.ticketsales.core.domain.model.Purchase

/**
 * A maquininha, vista pelo domínio. **Só dispara** (ADR 0002):
 * o desfecho chega depois, talvez num processo recriado, como um [PaymentOutcome] entregue ao
 * `HandlePaymentOutcomeUseCase`, ou como um [OrdersPage] entregue ao `ReconcilePurchaseUseCase`.
 */
interface PaymentTerminal {

    /** Checagem prévia, antes de criar a Purchase: o app de pagamento está instalado? Não garante o disparo. */
    fun isAvailable(): Boolean

    fun requestPayment(purchase: Purchase)

    /** Pede uma página (a partir de 1) da lista de Cielo Orders, para a Reconciliation. */
    fun requestOrders(page: Int)
}
