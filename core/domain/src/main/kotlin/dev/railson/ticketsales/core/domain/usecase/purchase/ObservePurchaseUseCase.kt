package dev.railson.ticketsales.core.domain.usecase.purchase

import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.Flow

/** Uma Purchase como está no disco: a tela acompanha o desfecho por aqui, nunca pelo callback. */
class ObservePurchaseUseCase(private val purchaseRepository: PurchaseRepository) {

    operator fun invoke(purchaseId: String): Flow<Purchase?> = purchaseRepository.observe(purchaseId)
}
