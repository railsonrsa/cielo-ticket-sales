package dev.railson.ticketsales.payment.cielolio.callback

import android.util.Log
import dev.railson.ticketsales.core.domain.terminal.ListedOrder
import dev.railson.ticketsales.core.domain.terminal.OrdersPage
import dev.railson.ticketsales.payment.cielolio.json.CieloJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * O `response` do `order://orders`, em Base64, traduzido para uma [OrdersPage].
 * `null` quando a consulta não respondeu de verdade (`{code, reason}`, Base64 ou JSON inválido): a Reconciliation
 * trata como inconclusiva, nunca como "nenhuma Order".
 */
internal object OrdersResponseTranslator {

    private const val TAG = "CieloLio"

    /** O `createdAt` vem no formato padrão do Gson em inglês, no fuso do aparelho: `Oct 17, 2026 9:16:30 PM`. */
    private const val CREATED_AT_PATTERN = "MMM d, yyyy h:mm:ss a"

    fun translate(response: String?, timeZone: TimeZone = TimeZone.getDefault()): OrdersPage? {
        val json = response?.let(CieloJson::decode) ?: return failed("sem response ou Base64/JSON inválido")
        if ("results" !in json || "totalPages" !in json) return failed("formato desconhecido")
        return runCatching {
            OrdersPage(
                number = json.getValue("currentPage").jsonPrimitive.int,
                totalPages = json.getValue("totalPages").jsonPrimitive.int,
                orders = json.getValue("results").jsonArray.map { listedOrder(it.jsonObject, timeZone) },
            )
        }.getOrElse { failed("campos fora do formato: $it") }
    }

    private fun listedOrder(order: JsonObject, timeZone: TimeZone) = ListedOrder(
        reference = CieloJson.reference(order),
        createdAtEpochMillis = order["createdAt"]?.jsonPrimitive?.contentOrNull?.let { createdAt(it, timeZone) },
        cieloOrder = CieloJson.cieloOrder(order),
    )

    /** Sem data legível a Order continua valendo; só o atalho de parada da busca deixa de ser usado. */
    private fun createdAt(text: String, timeZone: TimeZone): Long? {
        val format = SimpleDateFormat(CREATED_AT_PATTERN, Locale.US).apply {
            this.timeZone = timeZone
            isLenient = false
        }
        // Formatadores mais novos separam a hora do AM/PM com um espaço estreito, e não com o comum.
        val normalized = text.replace(' ', ' ').replace(' ', ' ')
        return format.parse(normalized, ParsePosition(0))?.time
    }

    private fun failed(why: String): OrdersPage? {
        Log.w(TAG, "Consulta de Orders sem resposta legível ($why): a verificação fica inconclusiva")
        return null
    }
}
