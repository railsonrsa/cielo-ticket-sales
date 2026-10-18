package dev.railson.ticketsales.feature.purchases.presentation.purchases

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun PurchasesScreen(
    onBack: () -> Unit,
    onOpenReceipt: (purchaseId: String) -> Unit,
    viewModel: PurchasesViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PurchasesContent(uiState = uiState, onBack = onBack, onOpenReceipt = onOpenReceipt)
}
