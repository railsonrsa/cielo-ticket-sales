package dev.railson.ticketsales.core.domain.usecase.payment

import dev.railson.ticketsales.core.domain.model.Purchase

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

    /**
     * Purchases aprovadas fora de hora, que o Operator ainda não aceitou. O Cart não foi esvaziado por elas e pode
     * repetir os mesmos ingressos: "Revisar carrinho" ou "Continuar e pagar".
     */
    data class LateApprovalsToReview(val purchases: List<Purchase>) : StartPurchaseResult
}
