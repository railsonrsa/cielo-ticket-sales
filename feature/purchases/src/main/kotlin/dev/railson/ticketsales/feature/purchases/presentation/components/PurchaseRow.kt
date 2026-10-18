package dev.railson.ticketsales.feature.purchases.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.model.PurchaseStatus
import dev.railson.ticketsales.core.ui.format.formatBrl
import dev.railson.ticketsales.core.ui.format.formatDateTime
import dev.railson.ticketsales.feature.purchases.R
import dev.railson.ticketsales.feature.purchases.presentation.purchases.PendingRefundDetails

/** Uma linha de "Minhas compras". Sem [onClick], a linha não abre nada. Com [pendingRefund], ganha o selo vermelho. */
@Composable
internal fun PurchaseRow(
    purchase: Purchase,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    pendingRefund: PendingRefundDetails? = null,
) {
    val content: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            pendingRefund?.let { PendingRefundBadge(it) }
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

/** O selo vermelho, com o que o Operator precisa para estornar na maquininha. */
@Composable
private fun PendingRefundBadge(details: PendingRefundDetails) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Surface(color = MaterialTheme.colorScheme.error, shape = MaterialTheme.shapes.small) {
            Text(
                stringResource(R.string.purchases_pending_refund),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onError,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        val reason = when (details.reason) {
            null -> R.string.purchases_refund_duplicate
            FailureReason.AmountMismatch -> R.string.purchases_refund_amount_mismatch
            FailureReason.SoldOutAfterPayment -> R.string.purchases_refund_sold_out
            else -> R.string.purchases_refund_other
        }
        Text(stringResource(reason), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        details.refundedOrder?.let { order ->
            Text(
                stringResource(R.string.purchases_refund_order, formatBrl(order.authorizedAmount.cents), order.authCode, order.cieloCode),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(stringResource(R.string.purchases_refund_order_id, order.orderId), style = MaterialTheme.typography.bodySmall)
        }
        details.extraOrderIds.forEach { orderId ->
            Text(stringResource(R.string.purchases_refund_extra_order, orderId), style = MaterialTheme.typography.bodySmall)
        }
    }
}
