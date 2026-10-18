package dev.railson.ticketsales.feature.checkout.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.railson.ticketsales.core.domain.model.CapacityConflict
import dev.railson.ticketsales.core.domain.model.CartItem
import dev.railson.ticketsales.core.ui.format.formatBrl
import dev.railson.ticketsales.core.ui.format.formatDateTime
import dev.railson.ticketsales.feature.checkout.R

@Composable
internal fun CartItemCard(
    item: CartItem,
    onChangeQuantity: (Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val conflict = item.capacityConflict
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.event.name, style = MaterialTheme.typography.titleMedium)
            Text(formatDateTime(item.event.startsAtEpochMillis), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.cart_unit_price, formatBrl(item.event.unitPrice.cents)),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (conflict != null) CapacityConflictBadge(conflict)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Com conflito, só "Ajustar para N" ou "Remover" resolvem: qualquer outra quantidade ainda passa da Capacity.
                    OutlinedButton(onClick = { onChangeQuantity(item.quantity - 1) }, enabled = conflict == null && item.quantity > 1) {
                        Text(stringResource(R.string.cart_decrease))
                    }
                    Text(item.quantity.toString(), style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = { onChangeQuantity(item.quantity + 1) }, enabled = item.quantity < item.event.capacity) {
                        Text(stringResource(R.string.cart_increase))
                    }
                }
                Text(formatBrl(item.subtotal.cents), style = MaterialTheme.typography.titleSmall)
            }
            Row(modifier = Modifier.align(Alignment.End)) {
                if (conflict is CapacityConflict.OnlyLeft) {
                    TextButton(onClick = { onChangeQuantity(conflict.available) }) {
                        Text(stringResource(R.string.cart_adjust_to, conflict.available))
                    }
                }
                TextButton(onClick = onRemove) { Text(stringResource(R.string.cart_remove)) }
            }
        }
    }
}

@Composable
private fun CapacityConflictBadge(conflict: CapacityConflict) {
    Text(
        text = when (conflict) {
            CapacityConflict.SoldOut -> stringResource(R.string.cart_sold_out)
            is CapacityConflict.OnlyLeft -> pluralStringResource(R.plurals.cart_only_left, conflict.available, conflict.available)
        },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.error,
    )
}
