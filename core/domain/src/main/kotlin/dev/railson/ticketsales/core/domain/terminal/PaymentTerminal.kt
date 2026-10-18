package dev.railson.ticketsales.core.domain.terminal

import dev.railson.ticketsales.core.domain.model.Purchase

/**
 * A maquininha, vista pelo domínio. **Só dispara** (ADR 0002):
 * o desfecho chega depois, talvez num processo recriado, como um [PaymentOutcome] entregue ao
 * `HandlePaymentOutcomeUseCase`.
 */
interface PaymentTerminal {

    /** Checagem prévia, antes de criar a Purchase: o app de pagamento está instalado? Não garante o disparo. */
    fun isAvailable(): Boolean

    fun requestPayment(purchase: Purchase)
}
