package dev.railson.ticketsales.feature.checkout.presentation.cart

internal sealed interface CartAction {
    data class ChangeQuantity(val eventId: String, val quantity: Int) : CartAction
    data class Remove(val eventId: String) : CartAction
    data object Pay : CartAction

    /** "Tentar de novo" depois de erro ou recusa: uma Purchase nova, com outro `reference`, a partir do mesmo Cart. */
    data object Retry : CartAction
    data object DismissDialog : CartAction

    /** "Verificar pagamento": a Reconciliation da Purchase sem resposta. */
    data object CheckPayment : CartAction
    data object AskToAbandon : CartAction
    data object ConfirmAbandon : CartAction
}
