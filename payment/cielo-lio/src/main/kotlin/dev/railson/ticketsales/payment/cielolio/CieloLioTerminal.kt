package dev.railson.ticketsales.payment.cielolio

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.terminal.PaymentTerminal
import dev.railson.ticketsales.payment.cielolio.deeplink.PaymentDeepLink
import dev.railson.ticketsales.payment.cielolio.service.PaymentDispatchService

/**
 * A maquininha Cielo LIO, por Deep Link. Só dispara (ADR 0002): a resposta chega depois na `LioCallbackActivity`.
 */
internal class CieloLioTerminal(private val context: Context) : PaymentTerminal {

    override fun isAvailable(): Boolean =
        context.packageManager.resolveActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PROBE)), PackageManager.MATCH_DEFAULT_ONLY) != null

    override fun requestPayment(purchase: Purchase) {
        PaymentDispatchService.dispatch(context, PaymentDeepLink.payment(purchase))
    }

    private companion object {
        const val PROBE = "lio://payment"
    }
}
