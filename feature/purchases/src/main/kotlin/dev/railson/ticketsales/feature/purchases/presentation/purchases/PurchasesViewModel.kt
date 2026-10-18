package dev.railson.ticketsales.feature.purchases.presentation.purchases

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.railson.ticketsales.core.domain.usecase.payment.AbandonPurchaseUseCase
import dev.railson.ticketsales.core.domain.usecase.payment.AcknowledgeSoldOutUseCase
import dev.railson.ticketsales.core.domain.usecase.payment.ReconcilePurchaseUseCase
import dev.railson.ticketsales.core.domain.usecase.purchase.ObservePurchasesUseCase
import dev.railson.ticketsales.core.domain.usecase.purchase.ObserveSoldOutNoticesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * O desfecho da verificação chega pelo banco: a órfã sai do topo, ou ganha o aviso de verificação inconclusiva.
 * O esgotado após o pagamento também: o dialog fica até o "Entendi", e o selo de Pending Refund continua.
 */
internal class PurchasesViewModel(
    observePurchases: ObservePurchasesUseCase,
    private val reconcilePurchase: ReconcilePurchaseUseCase,
    private val abandonPurchase: AbandonPurchaseUseCase,
    observeSoldOutNotices: ObserveSoldOutNoticesUseCase,
    private val acknowledgeSoldOut: AcknowledgeSoldOutUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val uiState: StateFlow<PurchasesUiState> = combine(
        observePurchases(),
        reconcilePurchase.isRunning,
        savedStateHandle.getStateFlow(CONFIRMING_ABANDON, false),
        observeSoldOutNotices(),
    ) { purchases, checking, confirming, soldOutNotices ->
        PurchasesUiState(
            isLoading = false,
            purchases = purchases,
            isCheckingPayment = checking,
            pendingRefunds = purchases.mapNotNull { purchase -> purchase.pendingRefundDetails()?.let { purchase.id to it } }.toMap(),
            soldOutNotice = soldOutNotices.firstOrNull(),
        ).let { it.copy(confirmingAbandon = confirming && it.canAbandon) }
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
                is PurchasesAction.AcknowledgeSoldOut -> acknowledgeSoldOut(action.purchaseId)
            }
        }
    }

    private companion object {
        const val CONFIRMING_ABANDON = "confirming_abandon"
    }
}
