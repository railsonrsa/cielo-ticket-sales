package dev.railson.ticketsales.feature.purchases.presentation.receipt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.railson.ticketsales.core.domain.usecase.purchase.ObservePurchaseUseCase
import dev.railson.ticketsales.core.domain.usecase.purchase.ObserveTicketsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal class ReceiptViewModel(
    purchaseId: String,
    observePurchase: ObservePurchaseUseCase,
    observeTickets: ObserveTicketsUseCase,
) : ViewModel() {

    val uiState: StateFlow<ReceiptUiState> = combine(observePurchase(purchaseId), observeTickets(purchaseId)) { purchase, tickets ->
        ReceiptUiState(purchase = purchase, tickets = tickets)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReceiptUiState())
}
