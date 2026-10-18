package dev.railson.ticketsales.feature.checkout.presentation.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.railson.ticketsales.core.domain.usecase.cart.ChangeCartQuantityUseCase
import dev.railson.ticketsales.core.domain.usecase.cart.ObserveCartUseCase
import dev.railson.ticketsales.core.domain.usecase.cart.RemoveFromCartUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class CartViewModel(
    observeCart: ObserveCartUseCase,
    private val changeQuantity: ChangeCartQuantityUseCase,
    private val removeFromCart: RemoveFromCartUseCase,
) : ViewModel() {

    val uiState: StateFlow<CartUiState> = observeCart()
        .map { cart -> CartUiState(isLoading = false, cart = cart) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CartUiState())

    fun onAction(action: CartAction) {
        viewModelScope.launch {
            when (action) {
                is CartAction.ChangeQuantity -> changeQuantity(action.eventId, action.quantity)
                is CartAction.Remove -> removeFromCart(action.eventId)
            }
        }
    }
}
