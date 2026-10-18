package dev.railson.ticketsales.feature.purchases.presentation.purchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.railson.ticketsales.core.domain.usecase.purchase.ObservePurchasesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class PurchasesViewModel(observePurchases: ObservePurchasesUseCase) : ViewModel() {

    val uiState: StateFlow<PurchasesUiState> = observePurchases()
        .map { PurchasesUiState(isLoading = false, purchases = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PurchasesUiState())
}
