package dev.railson.ticketsales.payment.cielolio.callback

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dev.railson.ticketsales.core.domain.usecase.payment.HandlePaymentOutcomeUseCase
import dev.railson.ticketsales.core.domain.usecase.payment.ReconcilePurchaseUseCase
import dev.railson.ticketsales.payment.cielolio.service.PaymentDispatchService
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

/**
 * Onde as respostas da Cielo entram (ADR 0002): `order://response` (pagamento) e `order://orders` (lista de Orders
 * da Reconciliation). Sem UI: traduz a resposta, entrega ao use case e se fecha.
 * O processo pode ter acabado de ser recriado só para isto (cenário C9): tudo sai do intent e do disco, no `onCreate`.
 */
internal class LioCallbackActivity : ComponentActivity() {

    private val handleOutcome: HandlePaymentOutcomeUseCase by inject()
    private val reconcilePurchase: ReconcilePurchaseUseCase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent.data
        if (data?.host == ORDERS_HOST) onOrders(data) else onPayment(data)
    }

    private fun onPayment(data: Uri?) {
        PaymentDispatchService.stop(this)
        val outcome = PaymentResponseTranslator.translate(data?.getQueryParameter(RESPONSE))
        // O use case é idempotente: reprocessar numa recriação da Activity não muda nada.
        lifecycleScope.launch {
            withContext(NonCancellable) { handleOutcome(outcome) }
            // A tela observa o banco: basta trazer de volta a MainActivity que já existe (ou criar uma, se o
            // processo foi recriado). Sem CLEAR_TOP/SINGLE_TOP nasceria uma segunda por cima, de volta ao catálogo.
            packageManager.getLaunchIntentForPackage(packageName)?.let { launch ->
                startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            }
            finish()
        }
    }

    // A consulta é silenciosa: o app continua na frente, então não há o que reabrir. Uma página que chegue num
    // processo recriado não tem busca esperando por ela, e o use case a ignora.
    private fun onOrders(data: Uri) {
        val page = OrdersResponseTranslator.translate(data.getQueryParameter(RESPONSE))
        lifecycleScope.launch {
            withContext(NonCancellable) {
                if (page != null) reconcilePurchase.onOrdersPage(page) else reconcilePurchase.onOrdersFailed()
            }
            finish()
        }
    }

    private companion object {
        const val RESPONSE = "response"
        const val ORDERS_HOST = "orders"
    }
}
