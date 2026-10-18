package dev.railson.ticketsales.payment.cielolio.callback

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dev.railson.ticketsales.core.domain.usecase.payment.HandlePaymentOutcomeUseCase
import dev.railson.ticketsales.payment.cielolio.service.PaymentDispatchService
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

/**
 * Onde o `order://response` entra (ADR 0002). Sem UI: traduz a resposta, entrega ao use case, abre o app e se fecha.
 * O processo pode ter acabado de ser recriado só para isto (cenário C9): tudo sai do intent e do disco, no `onCreate`.
 */
internal class LioCallbackActivity : ComponentActivity() {

    private val handleOutcome: HandlePaymentOutcomeUseCase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PaymentDispatchService.stop(this)
        val outcome = PaymentResponseTranslator.translate(intent.data?.getQueryParameter(RESPONSE))
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

    private companion object {
        const val RESPONSE = "response"
    }
}
