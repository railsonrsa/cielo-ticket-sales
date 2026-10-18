package dev.railson.ticketsales.payment.cielolio

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.terminal.PaymentTerminal
import dev.railson.ticketsales.payment.cielolio.deeplink.OrdersDeepLink
import dev.railson.ticketsales.payment.cielolio.deeplink.PaymentDeepLink
import dev.railson.ticketsales.payment.cielolio.service.PaymentDispatchService

/**
 * A maquininha Cielo LIO, por Deep Link. Só dispara (ADR 0002): a resposta chega depois na `LioCallbackActivity`,
 * em `order://response` (pagamento) ou `order://orders` (lista de Orders).
 */
internal class CieloLioTerminal(private val context: Context) : PaymentTerminal {

    override fun isAvailable(): Boolean =
        context.packageManager.resolveActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PROBE)), PackageManager.MATCH_DEFAULT_ONLY) != null

    override fun requestPayment(purchase: Purchase) {
        PaymentDispatchService.dispatch(context, PaymentDeepLink.payment(purchase))
    }

    // A consulta é silenciosa (~40 ms) e não precisa do foreground service: sai direto, com o app na frente.
    override fun requestOrders(page: Int) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(OrdersDeepLink.orders(page))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            // Sem resposta, a Reconciliation termina inconclusiva pelo tempo limite.
            Log.w(TAG, "App da Cielo não encontrado para listar as Orders")
        }
    }

    private companion object {
        const val TAG = "CieloLio"
        const val PROBE = "lio://payment"
    }
}
