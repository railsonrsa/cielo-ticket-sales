package dev.railson.ticketsales.payment.cielolio.callback

import android.util.Log
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.terminal.PaymentOutcome
import dev.railson.ticketsales.payment.cielolio.json.CieloJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

/**
 * O `response` do `order://response`, em Base64, traduzido para o domínio **pela forma do JSON**.
 * Nunca pelo `responsecode` (vem `0` no sucesso e no cancelamento, e falta no erro), nem por `status` ou
 * `paidAmount` (vem `0` mesmo `PAID`).
 */
internal object PaymentResponseTranslator {

    private const val TAG = "CieloLio"

    private val FAILURE_CODES = mapOf(
        1 to FailureReason.CanceledByOperator,
        2 to FailureReason.PaymentError,
        3 to FailureReason.Declined,
        4 to FailureReason.TerminalAuthError,
    )

    fun translate(response: String?): PaymentOutcome {
        val json = response?.let(CieloJson::decode) ?: return unreadable("sem response ou Base64/JSON inválido")
        return runCatching {
            when {
                "id" in json && "payments" in json -> cieloOrder(json)
                "code" in json && "reason" in json -> failure(json)
                else -> unreadable("formato desconhecido")
            }
        }.getOrElse { unreadable("campos fora do formato: $it") }
    }

    private fun cieloOrder(json: JsonObject): PaymentOutcome {
        val reference = CieloJson.reference(json) ?: return unreadable("Order sem reference")
        return PaymentOutcome.Paid(reference = reference, cieloOrder = CieloJson.cieloOrder(json))
    }

    // Um código fora de 1–4 não é falha certa: a Purchase continua AwaitingPayment até a Reconciliation (ADR 0001).
    private fun failure(json: JsonObject): PaymentOutcome {
        val code = json.getValue("code").jsonPrimitive.int
        return FAILURE_CODES[code]?.let(PaymentOutcome::Failed) ?: unreadable("código de falha desconhecido: $code")
    }

    private fun unreadable(why: String): PaymentOutcome {
        Log.w(TAG, "Callback ilegível ($why): a Purchase continua aguardando pagamento")
        return PaymentOutcome.Unreadable
    }
}
