package dev.railson.ticketsales.feature.purchases.presentation.purchases

internal sealed interface PurchasesAction {

    /** "Verificar pagamento": a Reconciliation da Purchase sem resposta, no topo da lista. */
    data object CheckPayment : PurchasesAction
    data object AskToAbandon : PurchasesAction
    data object ConfirmAbandon : PurchasesAction
    data object DismissDialog : PurchasesAction

    /** O "Entendi" do dialog de esgotado após o pagamento. */
    data class AcknowledgeSoldOut(val purchaseId: String) : PurchasesAction
}
