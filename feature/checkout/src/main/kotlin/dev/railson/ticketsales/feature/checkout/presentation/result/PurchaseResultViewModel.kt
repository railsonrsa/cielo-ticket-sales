package dev.railson.ticketsales.feature.checkout.presentation.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.railson.ticketsales.core.domain.usecase.purchase.ObservePurchaseUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class PurchaseResultViewModel(
    purchaseId: String,
    observePurchase: ObservePurchaseUseCase,
) : ViewModel() {

    val uiState: StateFlow<PurchaseResultUiState> = observePurchase(purchaseId)
        .map { PurchaseResultUiState(purchase = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PurchaseResultUiState())
}
