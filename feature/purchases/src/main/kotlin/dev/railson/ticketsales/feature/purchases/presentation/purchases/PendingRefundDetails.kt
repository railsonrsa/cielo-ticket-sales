package dev.railson.ticketsales.feature.purchases.presentation.purchases

import dev.railson.ticketsales.core.domain.model.CieloOrder
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.model.PurchaseStatus

/**
 * O que o selo vermelho de Pending Refund mostra para o Operator estornar na maquininha.
 * [reason] é `null` na cobrança duplicada de uma Purchase aprovada: a [refundedOrder] fica, e só as
 * [extraOrderIds] voltam para o cliente. Nas `Failed`, a [refundedOrder] também volta.
 */
internal data class PendingRefundDetails(
    val reason: FailureReason?,
    val refundedOrder: CieloOrder?,
    val extraOrderIds: List<String>,
)

internal fun Purchase.pendingRefundDetails(): PendingRefundDetails? {
    if (!pendingRefund) return null
    return when (val status = status) {
        is PurchaseStatus.Failed -> PendingRefundDetails(status.reason, cieloOrder, extraOrderIds)
        else -> PendingRefundDetails(reason = null, refundedOrder = null, extraOrderIds = extraOrderIds)
    }
}
