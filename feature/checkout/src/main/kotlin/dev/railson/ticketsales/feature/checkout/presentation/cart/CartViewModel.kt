package dev.railson.ticketsales.feature.checkout.presentation.cart

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.model.PurchaseStatus
import dev.railson.ticketsales.core.domain.usecase.cart.ChangeCartQuantityUseCase
import dev.railson.ticketsales.core.domain.usecase.cart.ObserveCartUseCase
import dev.railson.ticketsales.core.domain.usecase.cart.RemoveFromCartUseCase
import dev.railson.ticketsales.core.domain.usecase.payment.StartPurchaseResult
import dev.railson.ticketsales.core.domain.usecase.payment.StartPurchaseUseCase
import dev.railson.ticketsales.core.domain.usecase.purchase.ObserveAwaitingPaymentUseCase
import dev.railson.ticketsales.core.domain.usecase.purchase.ObservePurchaseUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * O desfecho do pagamento chega pelo banco, nunca pelo callback: o ViewModel guarda só o id da Purchase que
 * disparou (no [SavedStateHandle], para sobreviver à recriação) e reage quando ela sai de `AwaitingPayment`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class CartViewModel(
    observeCart: ObserveCartUseCase,
    private val changeQuantity: ChangeCartQuantityUseCase,
    private val removeFromCart: RemoveFromCartUseCase,
    private val startPurchase: StartPurchaseUseCase,
    observeAwaitingPayment: ObserveAwaitingPaymentUseCase,
    observePurchase: ObservePurchaseUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val effectChannel = Channel<CartEffect>(Channel.BUFFERED)
    val effects: Flow<CartEffect> = effectChannel.receiveAsFlow()

    private val trackedPurchase: Flow<Purchase?> = savedStateHandle.getStateFlow<String?>(TRACKED_PURCHASE_ID, null)
        .flatMapLatest { purchaseId -> if (purchaseId == null) flowOf(null) else observePurchase(purchaseId) }

    val uiState: StateFlow<CartUiState> =
        combine(observeCart(), observeAwaitingPayment(), trackedPurchase) { cart, awaiting, tracked ->
            CartUiState(isLoading = false, cart = cart, awaitingPayment = awaiting != null, dialog = tracked?.dialog())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CartUiState())

    init {
        viewModelScope.launch {
            trackedPurchase.collect { purchase -> purchase?.oneOffEffect()?.let { sendAndStopTracking(it) } }
        }
    }

    fun onAction(action: CartAction) {
        viewModelScope.launch {
            when (action) {
                is CartAction.ChangeQuantity -> changeQuantity(action.eventId, action.quantity)
                is CartAction.Remove -> removeFromCart(action.eventId)
                CartAction.Pay -> pay()
                CartAction.Retry -> {
                    stopTracking()
                    pay()
                }
                CartAction.DismissDialog -> stopTracking()
            }
        }
    }

    private suspend fun pay() {
        // Conflito de Capacity e pagamento em andamento já aparecem na tela pelo próprio estado.
        val result = startPurchase()
        if (result is StartPurchaseResult.Started) savedStateHandle[TRACKED_PURCHASE_ID] = result.purchaseId
    }

    private suspend fun sendAndStopTracking(effect: CartEffect) {
        stopTracking()
        effectChannel.send(effect)
    }

    private fun stopTracking() {
        savedStateHandle[TRACKED_PURCHASE_ID] = null
    }

    private fun Purchase.oneOffEffect(): CartEffect? = when (status) {
        PurchaseStatus.Approved -> CartEffect.PurchaseApproved(id)
        PurchaseStatus.Failed(FailureReason.CanceledByOperator) -> CartEffect.PaymentCanceled
        else -> null
    }

    private fun Purchase.dialog(): CartDialog? = when (val status = status) {
        is PurchaseStatus.Failed -> when (status.reason) {
            FailureReason.PaymentError, FailureReason.Declined -> CartDialog.RetryablePaymentFailure(status.reason)
            FailureReason.TerminalAuthError -> CartDialog.TerminalAuthError
            FailureReason.AmountMismatch -> CartDialog.AmountMismatch
            else -> null
        }
        else -> null
    }

    private companion object {
        const val TRACKED_PURCHASE_ID = "tracked_purchase_id"
    }
}
