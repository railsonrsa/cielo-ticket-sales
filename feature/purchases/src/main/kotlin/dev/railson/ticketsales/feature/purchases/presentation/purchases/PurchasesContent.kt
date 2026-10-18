package dev.railson.ticketsales.feature.purchases.presentation.purchases

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.railson.ticketsales.core.domain.model.CieloOrder
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.model.Money
import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.model.PurchaseItem
import dev.railson.ticketsales.core.domain.model.PurchaseStatus
import dev.railson.ticketsales.core.ui.format.formatBrl
import dev.railson.ticketsales.core.ui.theme.TicketSalesTheme
import dev.railson.ticketsales.feature.purchases.R
import dev.railson.ticketsales.feature.purchases.presentation.components.PurchaseRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PurchasesContent(
    uiState: PurchasesUiState,
    onAction: (PurchasesAction) -> Unit,
    onBack: () -> Unit,
    onOpenReceipt: (purchaseId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    uiState.soldOutNotice?.let { SoldOutAfterPaymentDialog(it, onAction) }
    if (uiState.confirmingAbandon) ConfirmAbandonDialog(onAction)
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.purchases_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.purchases_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(innerPadding)
        when {
            uiState.isLoading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.purchases.isEmpty() -> Box(contentModifier, contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.purchases_empty))
            }
            else -> LazyColumn(
                modifier = contentModifier,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.purchases, key = Purchase::id) { purchase ->
                    if (purchase.status == PurchaseStatus.AwaitingPayment) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PurchaseRow(purchase = purchase, onClick = null)
                            AwaitingPaymentActions(uiState, onAction)
                        }
                    } else {
                        // Comprovante só de Purchase aprovada.
                        PurchaseRow(
                            purchase = purchase,
                            onClick = { onOpenReceipt(purchase.id) }.takeIf { purchase.status == PurchaseStatus.Approved },
                            pendingRefund = uiState.pendingRefunds[purchase.id],
                        )
                    }
                }
            }
        }
    }
}

/** A Reconciliation da Purchase sem resposta e, depois de uma verificação inconclusiva, a saída de emergência. */
@Composable
private fun AwaitingPaymentActions(uiState: PurchasesUiState, onAction: (PurchasesAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (uiState.canAbandon) {
            Text(stringResource(R.string.purchases_check_inconclusive), style = MaterialTheme.typography.bodyMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { onAction(PurchasesAction.CheckPayment) }, enabled = !uiState.isCheckingPayment) {
                Text(stringResource(if (uiState.isCheckingPayment) R.string.purchases_checking_payment else R.string.purchases_check_payment))
            }
            if (uiState.canAbandon) {
                TextButton(onClick = { onAction(PurchasesAction.AskToAbandon) }) { Text(stringResource(R.string.purchases_abandon)) }
            }
        }
    }
}

/** Bloqueante até o "Entendi": a Cielo cobrou, o Event esgotou e a cobrança precisa ser estornada. */
@Composable
private fun SoldOutAfterPaymentDialog(purchase: Purchase, onAction: (PurchasesAction) -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.purchases_sold_out_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.purchases_sold_out_body))
                purchase.cieloOrder?.let { order ->
                    Text(stringResource(R.string.purchases_refund_value, formatBrl(order.authorizedAmount.cents)))
                    Text(stringResource(R.string.purchases_auth_code, order.authCode))
                    Text(stringResource(R.string.purchases_cielo_code, order.cieloCode))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAction(PurchasesAction.AcknowledgeSoldOut(purchase.id)) }) {
                Text(stringResource(R.string.purchases_understood))
            }
        },
    )
}

@Composable
private fun ConfirmAbandonDialog(onAction: (PurchasesAction) -> Unit) {
    val dismiss = { onAction(PurchasesAction.DismissDialog) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(R.string.purchases_abandon_title)) },
        text = { Text(stringResource(R.string.purchases_abandon_body)) },
        confirmButton = {
            TextButton(onClick = { onAction(PurchasesAction.ConfirmAbandon) }) { Text(stringResource(R.string.purchases_abandon_confirm)) }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.purchases_abandon_keep_waiting)) } },
    )
}

@Preview
@Composable
private fun PurchasesContentPreview() {
    val show = PurchaseItem("1", "Noite de Choro no Coreto", 2, Money(4_000))
    TicketSalesTheme {
        PurchasesContent(
            uiState = PurchasesUiState(
                isLoading = false,
                purchases = listOf(
                    Purchase("a", PurchaseStatus.AwaitingPayment, listOf(show), Money(8_000), 1_792_000_300_000, inconclusiveChecks = 1),
                    Purchase(
                        "b",
                        PurchaseStatus.Failed(FailureReason.SoldOutAfterPayment),
                        listOf(show),
                        Money(8_000),
                        1_792_000_200_000,
                        cieloOrder = CieloOrder("9b1d-order", Money(8_000), cieloCode = "123456", authCode = "654321"),
                        pendingRefund = true,
                    ),
                    Purchase("c", PurchaseStatus.Approved, listOf(show), Money(8_000), 1_792_000_100_000),
                ),
                pendingRefunds = mapOf(
                    "b" to PendingRefundDetails(
                        FailureReason.SoldOutAfterPayment,
                        CieloOrder("9b1d-order", Money(8_000), cieloCode = "123456", authCode = "654321"),
                        extraOrderIds = emptyList(),
                    ),
                ),
            ),
            onAction = {},
            onBack = {},
            onOpenReceipt = {},
        )
    }
}
