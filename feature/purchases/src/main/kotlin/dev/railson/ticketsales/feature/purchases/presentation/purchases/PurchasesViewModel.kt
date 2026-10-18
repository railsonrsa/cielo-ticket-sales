package dev.railson.ticketsales.feature.purchases.presentation.purchases

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.railson.ticketsales.core.domain.usecase.payment.AbandonPurchaseUseCase
import dev.railson.ticketsales.core.domain.usecase.payment.ReconcilePurchaseUseCase
import dev.railson.ticketsales.core.domain.usecase.purchase.ObservePurchasesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** O desfecho da verificação chega pelo banco: a órfã sai do topo, ou ganha o aviso de verificação inconclusiva. */
internal class PurchasesViewModel(
    observePurchases: ObservePurchasesUseCase,
    private val reconcilePurchase: ReconcilePurchaseUseCase,
    private val abandonPurchase: AbandonPurchaseUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val uiState: StateFlow<PurchasesUiState> = combine(
        observePurchases(),
        reconcilePurchase.isRunning,
        savedStateHandle.getStateFlow(CONFIRMING_ABANDON, false),
    ) { purchases, checking, confirming ->
        PurchasesUiState(isLoading = false, purchases = purchases, isCheckingPayment = checking)
            .let { it.copy(confirmingAbandon = confirming && it.canAbandon) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PurchasesUiState())

    fun onAction(action: PurchasesAction) {
        viewModelScope.launch {
            when (action) {
                PurchasesAction.CheckPayment -> reconcilePurchase()
                PurchasesAction.AskToAbandon -> savedStateHandle[CONFIRMING_ABANDON] = true
                PurchasesAction.ConfirmAbandon -> {
                    savedStateHandle[CONFIRMING_ABANDON] = false
                    uiState.value.awaitingPayment?.let { abandonPurchase(it.id) }
                }
                PurchasesAction.DismissDialog -> savedStateHandle[CONFIRMING_ABANDON] = false
            }
        }
    }

    private companion object {
        const val CONFIRMING_ABANDON = "confirming_abandon"
    }
}
