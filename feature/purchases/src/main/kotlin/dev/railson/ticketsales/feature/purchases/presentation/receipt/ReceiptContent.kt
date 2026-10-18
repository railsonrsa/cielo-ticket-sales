package dev.railson.ticketsales.feature.purchases.presentation.receipt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.railson.ticketsales.core.domain.model.CieloOrder
import dev.railson.ticketsales.core.domain.model.Money
import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.model.PurchaseItem
import dev.railson.ticketsales.core.domain.model.PurchaseStatus
import dev.railson.ticketsales.core.domain.model.Ticket
import dev.railson.ticketsales.core.ui.format.formatBrl
import dev.railson.ticketsales.core.ui.format.formatDateTime
import dev.railson.ticketsales.core.ui.theme.TicketSalesTheme
import dev.railson.ticketsales.feature.purchases.R
import dev.railson.ticketsales.feature.purchases.presentation.components.TicketCard

/** O comprovante de uma Purchase aprovada: itens, total, data/hora, códigos da Cielo e um QR por Ticket. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReceiptContent(uiState: ReceiptUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.receipt_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.purchases_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        val purchase = uiState.purchase
        if (purchase == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Summary(purchase) }
                item { Text(stringResource(R.string.receipt_tickets), style = MaterialTheme.typography.titleMedium) }
                itemsIndexed(uiState.tickets, key = { _, ticket -> ticket.id }) { index, ticket ->
                    TicketCard(
                        ticket = ticket,
                        eventName = uiState.eventName(ticket),
                        position = index + 1,
                        count = uiState.tickets.size,
                    )
                }
            }
        }
    }
}

@Composable
private fun Summary(purchase: Purchase) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(formatDateTime(purchase.createdAtEpochMillis), style = MaterialTheme.typography.bodyMedium)
        purchase.items.forEach { item ->
            Text(stringResource(R.string.receipt_item, item.eventName, item.quantity, formatBrl(item.unitPrice.cents)))
        }
        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.receipt_total), style = MaterialTheme.typography.titleMedium)
            Text(formatBrl(purchase.total.cents), style = MaterialTheme.typography.titleMedium)
        }
        purchase.cieloOrder?.let { order ->
            Text(stringResource(R.string.receipt_auth_code, order.authCode), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.receipt_cielo_code, order.cieloCode), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview
@Composable
private fun ReceiptContentPreview() {
    TicketSalesTheme {
        ReceiptContent(
            uiState = ReceiptUiState(
                purchase = Purchase(
                    id = "3f2a9c1e",
                    status = PurchaseStatus.Approved,
                    items = listOf(PurchaseItem("1", "Noite de Choro no Coreto", 2, Money(4_000))),
                    total = Money(8_000),
                    createdAtEpochMillis = 1_792_000_000_000,
                    cieloOrder = CieloOrder("order-1", Money(8_000), cieloCode = "123456", authCode = "654321"),
                ),
                tickets = listOf(
                    Ticket("0b6e4c1a-6f1d-4d8e-9a57-2f1c3e5d7a90", "3f2a9c1e", "1"),
                    Ticket("7c2d9e4f-1a3b-4c5d-8e6f-9a0b1c2d3e4f", "3f2a9c1e", "1"),
                ),
            ),
            onBack = {},
        )
    }
}
