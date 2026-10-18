package dev.railson.ticketsales.core.domain.usecase.purchase

import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * As Purchases cobradas que esgotaram depois do pagamento e cujo dialog o Operator ainda não confirmou com o
 * "Entendi", da mais antiga para a mais recente.
 */
class ObserveSoldOutNoticesUseCase(private val purchaseRepository: PurchaseRepository) {

    operator fun invoke(): Flow<List<Purchase>> = purchaseRepository.observeAll().map { all ->
        all.filter(Purchase::isSoldOutNoticeToAcknowledge).sortedBy(Purchase::createdAtEpochMillis)
    }
}
