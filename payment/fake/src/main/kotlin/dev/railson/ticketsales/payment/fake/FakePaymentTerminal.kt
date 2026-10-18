package dev.railson.ticketsales.payment.fake

import android.content.Context
import android.content.Intent
import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.terminal.PaymentTerminal

/**
 * A maquininha simulada, para demonstrar o app sem o emulador da Cielo. Como a de verdade, só dispara:
 * abre a [FakePaymentActivity], e é ela que entrega o desfecho ao `HandlePaymentOutcomeUseCase`.
 */
internal class FakePaymentTerminal(private val context: Context) : PaymentTerminal {

    override fun isAvailable(): Boolean = true

    override fun requestPayment(purchase: Purchase) {
        context.startActivity(
            FakePaymentActivity.intent(context, purchase).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
