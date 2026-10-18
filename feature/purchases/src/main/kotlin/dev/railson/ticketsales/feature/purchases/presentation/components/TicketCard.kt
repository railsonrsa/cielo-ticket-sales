package dev.railson.ticketsales.feature.purchases.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.railson.ticketsales.core.domain.model.Ticket
import dev.railson.ticketsales.feature.purchases.R

/** Um Ticket com o QR. O id vai também em texto, para a portaria digitar se a leitura falhar. */
@Composable
internal fun TicketCard(ticket: Ticket, eventName: String, position: Int, count: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(eventName, style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.receipt_ticket_position, position, count), style = MaterialTheme.typography.bodySmall)
            QrCode(
                content = ticket.id,
                contentDescription = stringResource(R.string.receipt_ticket_qr, ticket.id),
                modifier = Modifier.widthIn(max = 240.dp).fillMaxWidth(),
            )
            Text(ticket.id, style = MaterialTheme.typography.labelSmall)
        }
    }
}
