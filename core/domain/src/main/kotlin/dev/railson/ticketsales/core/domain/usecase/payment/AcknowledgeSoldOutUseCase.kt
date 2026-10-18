package dev.railson.ticketsales.core.domain.usecase.payment

import dev.railson.ticketsales.core.domain.repository.PurchaseRepository

/** O "Entendi" do dialog de `SoldOutAfterPayment`: o dialog some, e a marca Pending Refund continua até o estorno. */
class AcknowledgeSoldOutUseCase(private val purchaseRepository: PurchaseRepository) {

    suspend operator fun invoke(purchaseId: String) {
        val purchase = purchaseRepository.find(purchaseId) ?: return
        if (purchase.isSoldOutNoticeToAcknowledge) purchaseRepository.acknowledge(purchaseId)
    }
}
