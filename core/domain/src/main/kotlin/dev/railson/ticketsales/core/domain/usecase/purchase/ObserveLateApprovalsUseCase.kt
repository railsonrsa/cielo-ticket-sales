package dev.railson.ticketsales.core.domain.usecase.purchase

import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** As Purchases aprovadas fora de hora que o Operator ainda não aceitou, da mais antiga para a mais recente. */
class ObserveLateApprovalsUseCase(private val purchaseRepository: PurchaseRepository) {

    operator fun invoke(): Flow<List<Purchase>> = purchaseRepository.observeAll().map { all ->
        all.filter(Purchase::isLateApprovalToAccept).sortedBy(Purchase::createdAtEpochMillis)
    }
}
