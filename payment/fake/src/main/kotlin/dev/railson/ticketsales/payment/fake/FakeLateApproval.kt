package dev.railson.ticketsales.payment.fake

import dev.railson.ticketsales.core.domain.terminal.PaymentOutcome
import dev.railson.ticketsales.core.domain.usecase.payment.HandlePaymentOutcomeUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * O desfecho atrasado da demo: a "Cielo" fecha sem responder e só cobra depois de [delayMillis]. Até lá, a Order
 * consta como `ENTERED`, então o "Verificar pagamento" dá a Purchase como `Failed(NotCompleted)`, e a aprovação
 * que chega depois cai na regra da aprovação atrasada. Fica só em memória: se o processo morrer, o callback se perde.
 */
internal class FakeLateApproval(
    private val cieloOrders: FakeCieloOrders,
    private val handleOutcome: HandlePaymentOutcomeUseCase,
    private val scope: CoroutineScope,
    private val delayMillis: Long = DELAY_MILLIS,
) {

    fun deliverLater(outcome: PaymentOutcome.Paid) {
        scope.launch {
            delay(delayMillis)
            cieloOrders.record(outcome)
            handleOutcome(outcome)
        }
    }

    companion object {
        const val DELAY_MILLIS = 30_000L
    }
}
