package dev.railson.ticketsales.feature.purchases.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.model.PurchaseStatus
import dev.railson.ticketsales.core.ui.format.formatBrl
import dev.railson.ticketsales.core.ui.format.formatDateTime
import dev.railson.ticketsales.feature.purchases.R

/** Uma linha de "Minhas compras". Sem [onClick], a linha não abre nada. Os selos de Pending Refund chegam no #19. */
@Composable
internal fun PurchaseRow(purchase: Purchase, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val content: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (purchase.status == PurchaseStatus.AwaitingPayment) {
                Text(
                    stringResource(R.string.purchases_awaiting_payment),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDateTime(purchase.createdAtEpochMillis), style = MaterialTheme.typography.titleSmall)
                Text(formatBrl(purchase.total.cents), style = MaterialTheme.typography.titleSmall)
            }
            Text(
                purchase.items.joinToString { it.eventName },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
            )
            Text(
                if (purchase.status == PurchaseStatus.Approved) {
                    pluralStringResource(R.plurals.purchases_tickets, purchase.ticketCount, purchase.ticketCount)
                } else {
                    stringResource(R.string.purchases_no_tickets)
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth()) { content() }
    } else {
        Card(modifier = modifier.fillMaxWidth()) { content() }
    }
}
