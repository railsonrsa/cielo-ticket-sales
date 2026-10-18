package dev.railson.ticketsales.feature.purchases.presentation.purchases

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.model.Money
import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.model.PurchaseItem
import dev.railson.ticketsales.core.domain.model.PurchaseStatus
import dev.railson.ticketsales.core.ui.theme.TicketSalesTheme
import dev.railson.ticketsales.feature.purchases.R
import dev.railson.ticketsales.feature.purchases.presentation.components.PurchaseRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PurchasesContent(
    uiState: PurchasesUiState,
    onBack: () -> Unit,
    onOpenReceipt: (purchaseId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    // Comprovante só de Purchase aprovada. A AwaitingPayment ganha o "Verificar pagamento" no #18.
                    PurchaseRow(
                        purchase = purchase,
                        onClick = { onOpenReceipt(purchase.id) }.takeIf { purchase.status == PurchaseStatus.Approved },
                    )
                }
            }
        }
    }
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
                    Purchase("a", PurchaseStatus.AwaitingPayment, listOf(show), Money(8_000), 1_792_000_300_000),
                    Purchase(
                        "b",
                        PurchaseStatus.Failed(FailureReason.SoldOutAfterPayment),
                        listOf(show),
                        Money(8_000),
                        1_792_000_200_000,
                        pendingRefund = true,
                    ),
                    Purchase("c", PurchaseStatus.Approved, listOf(show), Money(8_000), 1_792_000_100_000),
                ),
            ),
            onBack = {},
            onOpenReceipt = {},
        )
    }
}
