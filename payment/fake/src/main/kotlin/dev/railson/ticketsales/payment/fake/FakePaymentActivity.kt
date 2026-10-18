package dev.railson.ticketsales.payment.fake

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import dev.railson.ticketsales.core.domain.model.CieloOrder
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.model.Money
import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.terminal.PaymentOutcome
import dev.railson.ticketsales.core.domain.usecase.payment.HandlePaymentOutcomeUseCase
import dev.railson.ticketsales.core.ui.format.formatBrl
import dev.railson.ticketsales.core.ui.theme.TicketSalesTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.UUID
import kotlin.random.Random

/**
 * A "tela da Cielo" da fake: o Operator escolhe o desfecho. O Voltar fecha sem resposta, como na Cielo,
 * e a Purchase fica `AwaitingPayment`.
 */
internal class FakePaymentActivity : ComponentActivity() {

    private val handleOutcome: HandlePaymentOutcomeUseCase by inject()
    private val cieloOrders: FakeCieloOrders by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val purchaseId = checkNotNull(intent.getStringExtra(EXTRA_PURCHASE_ID))
        val total = Money(intent.getLongExtra(EXTRA_TOTAL_CENTS, 0))
        setContent {
            TicketSalesTheme {
                FakePaymentContent(
                    purchaseId = purchaseId,
                    total = total,
                    onOutcome = { outcome -> deliver(outcome) },
                )
            }
        }
    }

    private fun deliver(outcome: PaymentOutcome) {
        cieloOrders.record(outcome)
        lifecycleScope.launch {
            handleOutcome(outcome)
            finish()
        }
    }

    companion object {
        private const val EXTRA_PURCHASE_ID = "purchase_id"
        private const val EXTRA_TOTAL_CENTS = "total_cents"

        fun intent(context: Context, purchase: Purchase): Intent =
            Intent(context, FakePaymentActivity::class.java)
                .putExtra(EXTRA_PURCHASE_ID, purchase.id)
                .putExtra(EXTRA_TOTAL_CENTS, purchase.total.cents)
    }
}

@Composable
private fun FakePaymentContent(purchaseId: String, total: Money, onOutcome: (PaymentOutcome) -> Unit) {
    // Um toque só: um segundo desfecho seria um segundo callback.
    var delivered by remember { mutableStateOf(false) }
    val choose = { outcome: PaymentOutcome ->
        if (!delivered) {
            delivered = true
            onOutcome(outcome)
        }
    }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.fake_payment_title), style = MaterialTheme.typography.headlineSmall)
            Text(formatBrl(total.cents), style = MaterialTheme.typography.displaySmall)
            Text(stringResource(R.string.fake_payment_reference, purchaseId), style = MaterialTheme.typography.bodySmall)
            Button(onClick = { choose(paid(purchaseId, authorized = total)) }, enabled = !delivered, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.fake_payment_approve))
            }
            FAILURES.forEach { (reason, label) ->
                OutlinedButton(onClick = { choose(PaymentOutcome.Failed(reason)) }, enabled = !delivered, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(label))
                }
            }
            OutlinedButton(
                onClick = { choose(paid(purchaseId, authorized = Money(total.cents / 2))) },
                enabled = !delivered,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.fake_payment_amount_mismatch))
            }
            OutlinedButton(onClick = { choose(PaymentOutcome.Unreadable) }, enabled = !delivered, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.fake_payment_unreadable))
            }
            Text(stringResource(R.string.fake_payment_back_hint), style = MaterialTheme.typography.bodySmall)
        }
    }
}

private val FAILURES = listOf(
    FailureReason.CanceledByOperator to R.string.fake_payment_cancel,
    FailureReason.PaymentError to R.string.fake_payment_error,
    FailureReason.Declined to R.string.fake_payment_declined,
    FailureReason.TerminalAuthError to R.string.fake_payment_auth_error,
)

private fun paid(purchaseId: String, authorized: Money) = PaymentOutcome.Paid(
    reference = purchaseId,
    cieloOrder = CieloOrder(
        orderId = UUID.randomUUID().toString(),
        authorizedAmount = authorized,
        cieloCode = randomDigits(),
        authCode = randomDigits(),
    ),
)

private fun randomDigits() = Random.nextInt(100_000, 1_000_000).toString()

@Preview
@Composable
private fun FakePaymentContentPreview() {
    TicketSalesTheme {
        FakePaymentContent(purchaseId = "3f2a9c1e-0000-4000-8000-000000000000", total = Money(9_000), onOutcome = {})
    }
}
