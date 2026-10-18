package dev.railson.ticketsales.feature.catalog.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.railson.ticketsales.core.domain.model.Event
import dev.railson.ticketsales.core.ui.format.formatBrl
import dev.railson.ticketsales.core.ui.format.formatDateTime
import dev.railson.ticketsales.feature.catalog.R

@Composable
internal fun EventCard(event: Event, canAdd: Boolean, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(event.name, style = MaterialTheme.typography.titleMedium)
            Text(formatDateTime(event.startsAtEpochMillis), style = MaterialTheme.typography.bodyMedium)
            Text(event.venue, style = MaterialTheme.typography.bodyMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatBrl(event.unitPrice.cents), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = if (event.capacity == 0) {
                        stringResource(R.string.catalog_sold_out)
                    } else {
                        pluralStringResource(R.plurals.catalog_capacity, event.capacity, event.capacity)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (event.capacity == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onAdd, enabled = canAdd, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.catalog_add_to_cart))
            }
        }
    }
}
