package dev.railson.ticketsales.core.domain.model

/**
 * Uma tentativa de compra, cobrada num único pagamento. O [id] vai como `reference` para a Cielo.
 * Os [items] guardam o preço do momento do "Pagar": mudar o preço do Event depois não altera a Purchase.
 * [extraOrderIds] são as Cielo Orders pagas a mais (cobrança duplicada), que justificam o [pendingRefund].
 * [inconclusiveChecks] conta as Reconciliations sem resposta: depois da primeira, o Operator pode desistir.
 * [needsAcknowledgment] marca um desfecho que chegou fora de hora e que o Operator ainda não reconheceu: a aprovação
 * atrasada (confirmada no próximo "Pagar") ou o `SoldOutAfterPayment` (confirmado no "Entendi").
 */
data class Purchase(
    val id: String,
    val status: PurchaseStatus,
    val items: List<PurchaseItem>,
    val total: Money,
    val createdAtEpochMillis: Long,
    val cieloOrder: CieloOrder? = null,
    val pendingRefund: Boolean = false,
    val extraOrderIds: List<String> = emptyList(),
    val inconclusiveChecks: Int = 0,
    val needsAcknowledgment: Boolean = false,
) {

    val ticketCount: Int get() = items.sumOf(PurchaseItem::quantity)

    /** "Desistir desta compra" só vale para a órfã que já teve ao menos uma verificação inconclusiva (ADR 0001, item 7). */
    val canBeAbandoned: Boolean get() = status == PurchaseStatus.AwaitingPayment && inconclusiveChecks > 0

    /** Aprovada fora de hora e ainda não aceita: o próximo "Pagar" pede para revisar o Cart. */
    val isLateApprovalToAccept: Boolean get() = status == PurchaseStatus.Approved && needsAcknowledgment

    /** Esgotou depois do pagamento e o Operator ainda não tocou em "Entendi". */
    val isSoldOutNoticeToAcknowledge: Boolean
        get() = status == PurchaseStatus.Failed(FailureReason.SoldOutAfterPayment) && needsAcknowledgment
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
