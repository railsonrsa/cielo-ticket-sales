package dev.railson.ticketsales.core.domain.usecase.purchase

import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.Flow

/** A única Purchase `AwaitingPayment`, se houver. Enquanto ela existir, nenhuma venda nova começa. */
class ObserveAwaitingPaymentUseCase(private val purchaseRepository: PurchaseRepository) {

    operator fun invoke(): Flow<Purchase?> = purchaseRepository.observeAwaitingPayment()
}
