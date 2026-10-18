package dev.railson.ticketsales.payment.cielolio.callback

import android.util.Base64
import android.util.Log
import dev.railson.ticketsales.core.domain.model.CieloOrder
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.model.Money
import dev.railson.ticketsales.core.domain.terminal.PaymentOutcome
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/**
 * O `response` do `order://response`, em Base64, traduzido para o domínio **pela forma do JSON**.
 * Nunca pelo `responsecode` (vem `0` no sucesso e no cancelamento, e falta no erro), nem por `status` ou
 * `paidAmount` (vem `0` mesmo `PAID`).
 */
internal object PaymentResponseTranslator {

    private const val TAG = "CieloLio"

    /** `paymentFields.statusCode` de um pagamento autorizado: 0 = Pix, 1 = cartão. Vem como string. */
    private val AUTHORIZED_STATUS_CODES = setOf("0", "1")

    private val FAILURE_CODES = mapOf(
        1 to FailureReason.CanceledByOperator,
        2 to FailureReason.PaymentError,
        3 to FailureReason.Declined,
        4 to FailureReason.TerminalAuthError,
    )

    fun translate(response: String?): PaymentOutcome {
        val json = response?.let(::decode) ?: return unreadable("sem response ou Base64/JSON inválido")
        return runCatching {
            when {
                "id" in json && "payments" in json -> cieloOrder(json)
                "code" in json && "reason" in json -> failure(json)
                else -> unreadable("formato desconhecido")
            }
        }.getOrElse { unreadable("campos fora do formato: $it") }
    }

    private fun cieloOrder(json: JsonObject): PaymentOutcome {
        val reference = json["reference"]?.jsonPrimitive?.contentOrNull
        if (reference.isNullOrEmpty()) return unreadable("Order sem reference")
        val payments = json.getValue("payments").jsonArray.map { it.jsonObject }
        val authorized = payments.filter { it["paymentFields"]?.jsonObject?.get("statusCode")?.jsonPrimitive?.contentOrNull in AUTHORIZED_STATUS_CODES }
        val receipt = authorized.firstOrNull() ?: payments.firstOrNull()
        return PaymentOutcome.Paid(
            reference = reference,
            cieloOrder = CieloOrder(
                orderId = json.getValue("id").jsonPrimitive.content,
                authorizedAmount = Money(authorized.sumOf { it.getValue("amount").jsonPrimitive.long }),
                cieloCode = receipt?.get("cieloCode")?.jsonPrimitive?.contentOrNull.orEmpty(),
                authCode = receipt?.get("authCode")?.jsonPrimitive?.contentOrNull.orEmpty(),
            ),
        )
    }

    // Um código fora de 1–4 não é falha certa: a Purchase continua AwaitingPayment até a Reconciliation (ADR 0001).
    private fun failure(json: JsonObject): PaymentOutcome {
        val code = json.getValue("code").jsonPrimitive.int
        return FAILURE_CODES[code]?.let(PaymentOutcome::Failed) ?: unreadable("código de falha desconhecido: $code")
    }

    private fun decode(base64: String): JsonObject? = runCatching {
        // A leitura da query troca `+` por espaço, e o decoder do Android pularia o espaço sem reclamar.
        val bytes = Base64.decode(base64.replace(' ', '+'), Base64.DEFAULT)
        Json.parseToJsonElement(String(bytes, Charsets.UTF_8)).jsonObject
    }.getOrNull()

    private fun unreadable(why: String): PaymentOutcome {
        Log.w(TAG, "Callback ilegível ($why): a Purchase continua aguardando pagamento")
        return PaymentOutcome.Unreadable
    }
}
