package dev.railson.ticketsales.feature.checkout.presentation.result

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun PurchaseResultScreen(
    purchaseId: String,
    onNewSale: () -> Unit,
    onOpenReceipt: (purchaseId: String) -> Unit,
    viewModel: PurchaseResultViewModel = koinViewModel { parametersOf(purchaseId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Voltar daqui não leva ao Cart, que já foi esvaziado: é uma venda nova.
    BackHandler(onBack = onNewSale)
    PurchaseResultContent(uiState = uiState, onNewSale = onNewSale, onOpenReceipt = { onOpenReceipt(purchaseId) })
}
