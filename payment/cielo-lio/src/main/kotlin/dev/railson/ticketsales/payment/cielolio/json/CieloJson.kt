package dev.railson.ticketsales.payment.cielolio.json

import android.util.Base64
import dev.railson.ticketsales.core.domain.model.CieloOrder
import dev.railson.ticketsales.core.domain.model.Money
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/** O que as respostas da Cielo têm em comum: o `response` em Base64 e a Cielo Order com os pagamentos. */
internal object CieloJson {

    /** `paymentFields.statusCode` de um pagamento autorizado: 0 = Pix, 1 = cartão. Vem como string. */
    private val AUTHORIZED_STATUS_CODES = setOf("0", "1")

    fun decode(base64: String): JsonObject? = runCatching {
        // A leitura da query troca `+` por espaço, e o decoder do Android pularia o espaço sem reclamar.
        val bytes = Base64.decode(base64.replace(' ', '+'), Base64.DEFAULT)
        Json.parseToJsonElement(String(bytes, Charsets.UTF_8)).jsonObject
    }.getOrNull()

    fun reference(order: JsonObject): String? = order["reference"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotEmpty)

    /**
     * A Order com a soma **só dos pagamentos autorizados**. Nunca `status` nem `paidAmount`, que vem `0` mesmo `PAID`.
     * Uma Order que só entrou (`ENTERED`) não tem pagamento autorizado e soma zero.
     */
    fun cieloOrder(order: JsonObject): CieloOrder {
        val payments = order.getValue("payments").jsonArray.map { it.jsonObject }
        val authorized = payments.filter { it["paymentFields"]?.jsonObject?.get("statusCode")?.jsonPrimitive?.contentOrNull in AUTHORIZED_STATUS_CODES }
        val receipt = authorized.firstOrNull() ?: payments.firstOrNull()
        return CieloOrder(
            orderId = order.getValue("id").jsonPrimitive.content,
            authorizedAmount = Money(authorized.sumOf { it.getValue("amount").jsonPrimitive.long }),
            cieloCode = receipt?.get("cieloCode")?.jsonPrimitive?.contentOrNull.orEmpty(),
            authCode = receipt?.get("authCode")?.jsonPrimitive?.contentOrNull.orEmpty(),
        )
    }
}
