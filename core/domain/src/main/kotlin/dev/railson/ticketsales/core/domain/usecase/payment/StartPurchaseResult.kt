package dev.railson.ticketsales.core.domain.usecase.payment

/** O que aconteceu ao tocar em "Pagar". Só [Started] cria uma Purchase. */
sealed interface StartPurchaseResult {

    data class Started(val purchaseId: String) : StartPurchaseResult

    /** Já existe uma Purchase `AwaitingPayment`: nenhuma venda nova até ela ter um desfecho. */
    data object PaymentInProgress : StartPurchaseResult

    data object EmptyCart : StartPurchaseResult

    /** O app de pagamento da Cielo não está instalado: nenhuma Purchase é criada. */
    data object TerminalUnavailable : StartPurchaseResult

    /** Os Events do Cart que não cabem mais na Capacity atual. */
    data class CapacityConflicts(val eventIds: List<String>) : StartPurchaseResult
}
