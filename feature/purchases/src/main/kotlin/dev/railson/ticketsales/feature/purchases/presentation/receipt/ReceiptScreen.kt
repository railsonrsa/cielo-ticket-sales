package dev.railson.ticketsales.feature.purchases.presentation.receipt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun ReceiptScreen(
    purchaseId: String,
    onBack: () -> Unit,
    viewModel: ReceiptViewModel = koinViewModel { parametersOf(purchaseId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReceiptContent(uiState = uiState, onBack = onBack)
}
