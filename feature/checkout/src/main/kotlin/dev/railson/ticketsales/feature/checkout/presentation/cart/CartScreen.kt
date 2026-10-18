package dev.railson.ticketsales.feature.checkout.presentation.cart

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import dev.railson.ticketsales.feature.checkout.R
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun CartScreen(
    onBack: () -> Unit,
    onPurchaseApproved: (purchaseId: String) -> Unit,
    viewModel: CartViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val paymentCanceled = stringResource(R.string.cart_payment_canceled)
    val currentOnPurchaseApproved by rememberUpdatedState(onPurchaseApproved)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    CartEffect.PaymentCanceled -> snackbarHostState.showSnackbar(paymentCanceled)
                    is CartEffect.PurchaseApproved -> currentOnPurchaseApproved(effect.purchaseId)
                }
            }
        }
    }
    CartContent(uiState = uiState, onAction = viewModel::onAction, onBack = onBack, snackbarHostState = snackbarHostState)
}
