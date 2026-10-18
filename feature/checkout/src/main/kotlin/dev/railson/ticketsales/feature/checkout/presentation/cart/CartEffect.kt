package dev.railson.ticketsales.feature.checkout.presentation.cart

internal sealed interface CartEffect {
    data object PaymentCanceled : CartEffect
    data class PurchaseApproved(val purchaseId: String) : CartEffect
}
