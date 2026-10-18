package dev.railson.ticketsales.feature.purchases.presentation.purchases

import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.model.PurchaseStatus

/** As [purchases] já chegam filtradas e na ordem de "Minhas compras". */
internal data class PurchasesUiState(
    val isLoading: Boolean = true,
    val purchases: List<Purchase> = emptyList(),
    /** O "Verificar pagamento" está consultando a Cielo. */
    val isCheckingPayment: Boolean = false,
    /** A confirmação do "Desistir desta compra" está aberta. */
    val confirmingAbandon: Boolean = false,
) {

    /** A `AwaitingPayment` vem sempre no topo. */
    val awaitingPayment: Purchase? get() = purchases.firstOrNull()?.takeIf { it.status == PurchaseStatus.AwaitingPayment }

    /** A órfã já teve uma verificação inconclusiva: aviso de nova tentativa e o "Desistir desta compra". */
    val canAbandon: Boolean get() = awaitingPayment?.canBeAbandoned == true && !isCheckingPayment
}
