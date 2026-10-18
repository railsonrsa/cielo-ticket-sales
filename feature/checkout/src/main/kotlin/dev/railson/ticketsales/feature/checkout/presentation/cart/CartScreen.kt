package dev.railson.ticketsales.feature.checkout.presentation.cart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun CartScreen(onBack: () -> Unit, viewModel: CartViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CartContent(uiState = uiState, onAction = viewModel::onAction, onBack = onBack)
}
