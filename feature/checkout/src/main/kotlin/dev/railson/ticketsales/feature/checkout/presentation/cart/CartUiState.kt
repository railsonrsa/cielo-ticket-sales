package dev.railson.ticketsales.feature.checkout.presentation.cart

import dev.railson.ticketsales.core.domain.model.Cart
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.model.Purchase

internal data class CartUiState(
    val isLoading: Boolean = true,
    val cart: Cart = Cart(items = emptyList()),
    /** Há uma Purchase `AwaitingPayment`: banner "Pagamento sem resposta" e nenhuma venda nova. */
    val awaitingPayment: Boolean = false,
    /** O "Verificar pagamento" está consultando a Cielo. */
    val isCheckingPayment: Boolean = false,
    /** A órfã já teve uma verificação inconclusiva: aviso de nova tentativa e o "Desistir desta compra". */
    val canAbandon: Boolean = false,
    /** Há Purchases aprovadas fora de hora que o Operator ainda não aceitou: o Cart pode repetir os ingressos delas. */
    val hasLateApprovals: Boolean = false,
    val dialog: CartDialog? = null,
) {

    /** Um item em conflito com a Capacity, ou um pagamento sem resposta, bloqueia o "Pagar". */
    val canPay: Boolean get() = cart.items.isNotEmpty() && !cart.hasCapacityConflict && !awaitingPayment
}

/** A falha da última Purchase que precisa de uma decisão do Operator. */
internal sealed interface CartDialog {

    /** Erro genérico (2) ou recusa (3): dá para tentar de novo. */
    data class RetryablePaymentFailure(val reason: FailureReason) : CartDialog

    /** Código 4: tentar de novo não adianta. */
    data object TerminalAuthError : CartDialog

    /** O app de pagamento da Cielo não está instalado, na checagem prévia ou no disparo. */
    data object TerminalUnavailable : CartDialog

    /** A Cielo autorizou um valor diferente do total: nenhum Ticket, e a cobrança precisa ser estornada. */
    data object AmountMismatch : CartDialog

    /** A confirmação do "Desistir desta compra". */
    data object ConfirmAbandon : CartDialog

    /** O "Pagar" depois de uma aprovação atrasada: "Revisar carrinho" ou "Continuar e pagar". */
    data class LateApprovalsToReview(val purchases: List<Purchase>) : CartDialog

    /** Bloqueante até o "Entendi": a Cielo cobrou, o Event esgotou e a cobrança precisa ser estornada. */
    data class SoldOutAfterPayment(val purchase: Purchase) : CartDialog
}
