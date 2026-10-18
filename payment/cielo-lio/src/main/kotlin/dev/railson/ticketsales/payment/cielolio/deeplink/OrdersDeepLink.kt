package dev.railson.ticketsales.payment.cielolio.deeplink

import android.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * O `lio://orders` que lista as Cielo Orders, para a Reconciliation. A resposta volta num host próprio, e não no
 * `order://response`: um `{code, reason}` da consulta não pode ser lido como falha de pagamento.
 */
internal object OrdersDeepLink {

    const val CALLBACK = "order://orders"

    /** O máximo que a Cielo aceita. */
    const val PAGE_SIZE = 5

    fun orders(page: Int): String {
        val request = OrdersRequest(PaymentDeepLink.ACCESS_TOKEN, PaymentDeepLink.CLIENT_ID, PAGE_SIZE, page)
        val base64 = Base64.encodeToString(Json.encodeToString(request).toByteArray(Charsets.UTF_8), Base64.DEFAULT)
        return "lio://orders?request=$base64&urlCallback=$CALLBACK"
    }
}

@Serializable
internal data class OrdersRequest(
    val accessToken: String,
    val clientID: String,
    val pageSize: Int,
    val page: Int,
)
