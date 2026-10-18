package dev.railson.ticketsales.feature.checkout.presentation.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.railson.ticketsales.core.domain.model.CieloOrder
import dev.railson.ticketsales.core.domain.model.Money
import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.model.PurchaseItem
import dev.railson.ticketsales.core.domain.model.PurchaseStatus
import dev.railson.ticketsales.core.ui.format.formatBrl
import dev.railson.ticketsales.core.ui.format.formatDateTime
import dev.railson.ticketsales.core.ui.theme.TicketSalesTheme
import dev.railson.ticketsales.feature.checkout.R

/** O resumo da venda aprovada. O comprovante completo, com os Tickets e o QR, abre em "Ver comprovante". */
@Composable
internal fun PurchaseResultContent(
    uiState: PurchaseResultUiState,
    onNewSale: () -> Unit,
    onOpenReceipt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpenReceipt, enabled = uiState.purchase != null, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.result_open_receipt))
                    }
                    Button(onClick = onNewSale, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.result_new_sale))
                    }
                }
            }
        },
    ) { innerPadding ->
        val purchase = uiState.purchase
        if (purchase == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.result_title), style = MaterialTheme.typography.headlineSmall)
                Text(formatDateTime(purchase.createdAtEpochMillis), style = MaterialTheme.typography.bodyMedium)
                purchase.items.forEach { item ->
                    Text(stringResource(R.string.result_item, item.eventName, item.quantity, formatBrl(item.unitPrice.cents)))
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.cart_total), style = MaterialTheme.typography.titleMedium)
                    Text(formatBrl(purchase.total.cents), style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    pluralStringResource(R.plurals.result_tickets, purchase.ticketCount, purchase.ticketCount),
                    style = MaterialTheme.typography.titleSmall,
                )
                purchase.cieloOrder?.let { order ->
                    Text(stringResource(R.string.result_auth_code, order.authCode), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.result_cielo_code, order.cieloCode), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Preview
@Composable
private fun PurchaseResultContentPreview() {
    TicketSalesTheme {
        PurchaseResultContent(
            uiState = PurchaseResultUiState(
                purchase = Purchase(
                    id = "3f2a9c1e",
                    status = PurchaseStatus.Approved,
                    items = listOf(
                        PurchaseItem("1", "Noite de Choro no Coreto", 2, Money(4_000)),
                        PurchaseItem("2", "Voz e Violão: Sessão Intimista", 1, Money(4_550)),
                    ),
                    total = Money(12_550),
                    createdAtEpochMillis = 1_792_000_000_000,
                    cieloOrder = CieloOrder("order-1", Money(12_550), cieloCode = "123456", authCode = "654321"),
                ),
            ),
            onNewSale = {},
            onOpenReceipt = {},
        )
    }
}
