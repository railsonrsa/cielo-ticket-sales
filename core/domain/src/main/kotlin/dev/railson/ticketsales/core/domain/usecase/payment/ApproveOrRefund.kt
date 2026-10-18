package dev.railson.ticketsales.core.domain.usecase.payment

import dev.railson.ticketsales.core.domain.model.CieloOrder
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.repository.PurchaseRepository

/**
 * Aprova se houver Capacity para todos os itens. Senão, quem confirmou primeiro levou os lugares, e o dinheiro desta
 * Purchase vai para estorno (ADR 0001, item 6): tudo ou nada, sem aprovação parcial e sem Capacity abaixo de 0.
 */
internal suspend fun PurchaseRepository.approveOrRefund(
    purchaseId: String,
    cieloOrder: CieloOrder,
    extraOrderIds: List<String> = emptyList(),
    late: Boolean = false,
) {
    if (approve(purchaseId, cieloOrder, extraOrderIds, late)) return
    fail(purchaseId, FailureReason.SoldOutAfterPayment, cieloOrder, pendingRefund = true, extraOrderIds, needsAcknowledgment = true)
}
