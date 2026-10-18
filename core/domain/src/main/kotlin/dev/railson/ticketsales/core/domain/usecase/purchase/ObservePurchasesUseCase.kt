package dev.railson.ticketsales.core.domain.usecase.purchase

import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.model.PurchaseStatus
import dev.railson.ticketsales.core.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * As Purchases de "Minhas compras": a `AwaitingPayment` no topo, depois as com Pending Refund e por fim as
 * `Approved`, cada grupo da mais recente para a mais antiga. As `Failed` sem dinheiro envolvido ficam de fora.
 */
class ObservePurchasesUseCase(private val purchaseRepository: PurchaseRepository) {

    operator fun invoke(): Flow<List<Purchase>> = purchaseRepository.observeAll().map { all ->
        all.filter { it.status !is PurchaseStatus.Failed || it.pendingRefund }
            .sortedWith(compareBy(Purchase::listGroup).thenByDescending(Purchase::createdAtEpochMillis))
    }
}

private fun Purchase.listGroup(): Int = when {
    status == PurchaseStatus.AwaitingPayment -> 0
    pendingRefund -> 1
    else -> 2
}
