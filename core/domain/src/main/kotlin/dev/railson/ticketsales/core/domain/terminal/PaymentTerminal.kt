package dev.railson.ticketsales.core.domain.terminal

import dev.railson.ticketsales.core.domain.model.Purchase

/**
 * A maquininha, vista pelo domínio. **Só dispara** (ADR 0002):
 * o desfecho chega depois, talvez num processo recriado, como um [PaymentOutcome] entregue ao
 * `HandlePaymentOutcomeUseCase`.
 */
interface PaymentTerminal {

    fun requestPayment(purchase: Purchase)
}
