package dev.railson.ticketsales.payment.fake

import android.content.Context
import android.content.Intent
import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.terminal.PaymentTerminal
import dev.railson.ticketsales.core.domain.usecase.payment.ReconcilePurchaseUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A maquininha simulada, para demonstrar o app sem o emulador da Cielo. Como a de verdade, só dispara:
 * abre a [FakePaymentActivity], e é ela que entrega o desfecho ao `HandlePaymentOutcomeUseCase`. As páginas de
 * Orders saem da [FakeCieloOrders] e chegam ao [ReconcilePurchaseUseCase] depois, em outra coroutine, como um callback.
 */
internal class FakePaymentTerminal(
    private val context: Context,
    private val cieloOrders: FakeCieloOrders,
    private val reconcilePurchase: Lazy<ReconcilePurchaseUseCase>,
    private val scope: CoroutineScope,
) : PaymentTerminal {

    override fun isAvailable(): Boolean = true

    override fun requestPayment(purchase: Purchase) {
        cieloOrders.entered(purchase)
        context.startActivity(
            FakePaymentActivity.intent(context, purchase).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    override fun requestOrders(page: Int) {
        scope.launch { reconcilePurchase.value.onOrdersPage(cieloOrders.page(page)) }
    }
}
