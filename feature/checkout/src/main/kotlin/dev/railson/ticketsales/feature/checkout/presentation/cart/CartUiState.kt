package dev.railson.ticketsales.feature.checkout.presentation.cart

import dev.railson.ticketsales.core.domain.model.Cart

internal data class CartUiState(
    val isLoading: Boolean = true,
    val cart: Cart = Cart(items = emptyList()),
) {

    /** Um item em conflito com a Capacity bloqueia o "Pagar" até ser ajustado ou removido. */
    val canPay: Boolean get() = cart.items.isNotEmpty() && !cart.hasCapacityConflict
}
