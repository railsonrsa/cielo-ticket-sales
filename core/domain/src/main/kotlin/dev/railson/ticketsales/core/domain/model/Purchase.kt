package dev.railson.ticketsales.core.domain.model

/**
 * Uma tentativa de compra, cobrada num único pagamento. O [id] vai como `reference` para a Cielo.
 * Os [items] guardam o preço do momento do "Pagar": mudar o preço do Event depois não altera a Purchase.
 */
data class Purchase(
    val id: String,
    val status: PurchaseStatus,
    val items: List<PurchaseItem>,
    val total: Money,
    val createdAtEpochMillis: Long,
    val cieloOrder: CieloOrder? = null,
    val pendingRefund: Boolean = false,
) {

    val ticketCount: Int get() = items.sumOf(PurchaseItem::quantity)
}

/** Três estados; o motivo de uma falha é um dado, não um estado. */
sealed interface PurchaseStatus {

    data object AwaitingPayment : PurchaseStatus

    data object Approved : PurchaseStatus

    data class Failed(val reason: FailureReason) : PurchaseStatus
}

/** Uma linha da Purchase, com o nome e o preço unitário congelados no "Pagar". */
data class PurchaseItem(
    val eventId: String,
    val eventName: String,
    val quantity: Int,
    val unitPrice: Money,
)

/** Os dados da Order que a Cielo criou para cobrar a Purchase. [authorizedAmount] já soma só os pagamentos autorizados. */
data class CieloOrder(
    val orderId: String,
    val authorizedAmount: Money,
    val cieloCode: String,
    val authCode: String,
)
