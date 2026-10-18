package dev.railson.ticketsales.feature.checkout.presentation.cart

internal sealed interface CartAction {
    data class ChangeQuantity(val eventId: String, val quantity: Int) : CartAction
    data class Remove(val eventId: String) : CartAction
}
