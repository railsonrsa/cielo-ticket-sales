package dev.railson.lioprobe

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.text.InputType
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Sonda descartável para o roteiro docs/emulador/roteiro-de-verificacao.md.
 * Dispara lio:// e registra tudo o que volta no callback order://response,
 * na tela e no logcat (tag LIO_PROBE). O log fica em SharedPreferences
 * para sobreviver à morte do processo (C8/C9).
 */
class ProbeActivity : Activity() {

    private val prefs by lazy { getSharedPreferences("probe", Context.MODE_PRIVATE) }

    private lateinit var reference: EditText
    private lateinit var clientId: EditText
    private lateinit var accessToken: EditText
    private lateinit var useService: CheckBox
    private lateinit var valueAsString: CheckBox
    private lateinit var logView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        if (savedInstanceState == null) log("Activity criada · pid=${Process.myPid()}")
        intent?.let(::handleCallback)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleCallback(intent)
    }

    // region Pedidos

    private fun pay(items: List<Item>, value: Long) {
        val request = credentials()
            .put("reference", reference.text.toString())
            .put("email", "teste@exemplo.com")
            .put("installments", 0)
            .put("paymentCode", "CREDITO_AVISTA")
            .put("items", JSONArray(items.map(Item::toJson)))
        if (valueAsString.isChecked) request.put("value", value.toString()) else request.put("value", value)
        prefs.edit().putString("lastReference", reference.text.toString()).apply()
        launch("payment", request)
    }

    private fun queryLastOrder() {
        val orderId = prefs.getString("lastOrderId", null)
            ?: return log("Nenhum orderId recebido ainda: rode um pagamento aprovado primeiro.")
        launch("order", credentials().put("orderId", orderId))
    }

    private fun listOrders() {
        launch("orders", credentials().put("pageSize", 5).put("page", 1))
    }

    private fun credentials() = JSONObject()
        .put("clientID", clientId.text.toString())
        .put("accessToken", accessToken.text.toString())

    private fun launch(action: String, request: JSONObject) {
        // Igual ao sample oficial: Base64.DEFAULT, sem escapar, direto na URI.
        val base64 = Base64.encodeToString(request.toString().toByteArray(Charsets.UTF_8), Base64.DEFAULT)
        val uri = "lio://$action?request=$base64&urlCallback=$CALLBACK"
        log("→ lio://$action · FGS=${useService.isChecked} · pid=${Process.myPid()}\n${request.toString(2)}")
        prefs.edit().putLong("sentAt", System.currentTimeMillis()).apply()
        if (useService.isChecked) PaymentForegroundService.start(this)
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        } catch (e: ActivityNotFoundException) {
            log("✗ ActivityNotFoundException: o emulador Cielo está instalado?\n$e")
            PaymentForegroundService.stop(this)
        }
    }

    // endregion

    // region Callback

    private fun handleCallback(intent: Intent) {
        val data = intent.data ?: return
        if (data.scheme != "order") return
        PaymentForegroundService.stop(this)
        setIntent(Intent(intent).setData(null)) // não reprocessar em recriação

        val sentAt = prefs.getLong("sentAt", 0L)
        val elapsed = if (sentAt > 0) "${System.currentTimeMillis() - sentAt} ms" else "?"
        val params = data.queryParameterNames.joinToString("\n") { name ->
            val value = data.getQueryParameter(name).orEmpty()
            "  $name = ${if (value.length > 80) value.take(80) + "… (${value.length} chars)" else value}"
        }
        val decoded = data.getQueryParameter("response")?.let(::decode) ?: "(sem parâmetro response)"
        log("← callback · ${elapsed} · pid=${Process.myPid()}\nURI: ${data.scheme}://${data.host}\nparâmetros:\n$params\nresponse:\n$decoded")
        rememberOrderId(decoded)
        reference.setText(UUID.randomUUID().toString())
    }

    private fun decode(base64: String): String {
        val text = runCatching { String(Base64.decode(base64, Base64.DEFAULT), Charsets.UTF_8) }
            .getOrElse { return "✗ Base64 inválido: $it" }
        return runCatching { JSONObject(text).toString(2) }.getOrElse { text }
    }

    private fun rememberOrderId(decoded: String) {
        val json = runCatching { JSONObject(decoded) }.getOrNull() ?: return
        if (json.has("id") && json.has("payments")) {
            prefs.edit().putString("lastOrderId", json.getString("id")).apply()
            log("formato: Order (sucesso) · orderId salvo para o C11")
        } else if (json.has("code")) {
            log("formato: erro {code=${json.opt("code")}, reason=${json.opt("reason")}}")
        }
    }

    // endregion

    // region UI e log

    private fun buildUi() {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        reference = field(column, "reference", UUID.randomUUID().toString())
        row(column, button("Novo UUID") { reference.setText(UUID.randomUUID().toString()) },
            button("Repetir último (C5)") { reference.setText(prefs.getString("lastReference", "")) })
        clientId = field(column, "clientID", "xxxxxxxx")
        accessToken = field(column, "accessToken", "xxxxxxxx")
        useService = CheckBox(this).apply { text = "Foreground service (desligar só no C9)"; isChecked = true }
        valueAsString = CheckBox(this).apply { text = "value como string (C10)" }
        column.addView(useService)
        column.addView(valueAsString)

        column.addView(button("C1–C5 · 1 item · R$ 50,00") { pay(listOf(Item("evt-1", "Show A", 1, 5000)), 5000) })
        column.addView(button("C6 · 2 Events · R$ 190,00") { pay(MULTI, 19000) })
        column.addView(button("C6b · 2 Events · value errado R$ 100,00") { pay(MULTI, 10000) })
        row(column, button("C11 · lio://order") { queryLastOrder() }, button("C12 · lio://orders") { listOrders() })
        column.addView(button("Limpar log") { prefs.edit().remove("log").apply(); logView.text = "" })

        logView = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 11f
            setTextIsSelectable(true)
            text = prefs.getString("log", "")
        }
        column.addView(logView)
        setContentView(ScrollView(this).apply { addView(column) })
    }

    private fun field(parent: LinearLayout, label: String, initial: String) = EditText(this).apply {
        hint = label
        setText(initial)
        inputType = InputType.TYPE_CLASS_TEXT
        parent.addView(TextView(context).apply { text = label })
        parent.addView(this)
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setOnClickListener { onClick() }
    }

    private fun row(parent: LinearLayout, vararg views: Button) {
        val row = LinearLayout(this)
        views.forEach { row.addView(it, LinearLayout.LayoutParams(0, -2, 1f)) }
        parent.addView(row)
    }

    private fun log(message: String) {
        val stamped = "[${SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())}] $message"
        message.chunked(3000).forEach { Log.i(TAG, it) }
        val all = stamped + "\n\n" + prefs.getString("log", "")
        prefs.edit().putString("log", all).commit() // commit: precisa estar em disco antes de um kill
        if (::logView.isInitialized) logView.text = all
    }

    // endregion

    private data class Item(val sku: String, val name: String, val quantity: Int, val unitPrice: Long) {
        fun toJson() = JSONObject()
            .put("sku", sku).put("name", name).put("quantity", quantity)
            .put("unitPrice", unitPrice).put("unitOfMeasure", "unidade")
    }

    private companion object {
        const val TAG = "LIO_PROBE"
        const val CALLBACK = "order://response"
        val MULTI = listOf(Item("evt-1", "Show A", 2, 5000), Item("evt-2", "Peça B", 3, 3000))
    }
}
