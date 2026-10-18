package dev.railson.ticketsales.payment.cielolio.deeplink

import android.util.Base64
import dev.railson.ticketsales.core.domain.model.Purchase
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** O `lio://payment` que pede à Cielo para cobrar uma Purchase. */
internal object PaymentDeepLink {

    const val CALLBACK = "order://response"

    // O emulador aceita credenciais falsas (cenário C4); um terminal real exigiria as do portal da Cielo.
    private const val CLIENT_ID = "xxxxxxxx"
    private const val ACCESS_TOKEN = "xxxxxxxx"

    fun payment(purchase: Purchase): String {
        val request = PaymentRequest(
            accessToken = ACCESS_TOKEN,
            clientID = CLIENT_ID,
            reference = purchase.id,
            // A Cielo cobra o `value` e ignora os itens (C6b): ele tem que ser a soma exata dos Purchase Items.
            value = purchase.items.sumOf { it.unitPrice.cents * it.quantity },
            items = purchase.items.map { PaymentRequest.Item(it.eventId, it.eventName, it.quantity, it.unitPrice.cents, UNIT) },
        )
        // Igual ao sample oficial e à sonda: Base64.DEFAULT, direto na URI.
        val base64 = Base64.encodeToString(Json.encodeToString(request).toByteArray(Charsets.UTF_8), Base64.DEFAULT)
        return "lio://payment?request=$base64&urlCallback=$CALLBACK"
    }

    private const val UNIT = "unidade"
}

@Serializable
internal data class PaymentRequest(
    val accessToken: String,
    val clientID: String,
    val reference: String,
    val value: Long,
    val items: List<Item>,
) {

    @Serializable
    data class Item(val sku: String, val name: String, val quantity: Int, val unitPrice: Long, val unitOfMeasure: String)
}
