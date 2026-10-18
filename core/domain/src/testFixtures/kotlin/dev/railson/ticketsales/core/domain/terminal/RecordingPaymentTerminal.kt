package dev.railson.ticketsales.core.domain.terminal

import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.repository.PurchaseRepository
import kotlinx.coroutines.runBlocking

/**
 * Maquininha que só anota o que foi disparado, e se a Purchase já estava em disco na hora do disparo
 * (a regra "gravar antes de disparar"). O desfecho o teste entrega pelo `HandlePaymentOutcomeUseCase`, e as
 * páginas de Orders pelo `ReconcilePurchaseUseCase`.
 */
class RecordingPaymentTerminal(private val purchaseRepository: PurchaseRepository) : PaymentTerminal {

    val requested = mutableListOf<Purchase>()
    val storedAtRequest = mutableListOf<Purchase?>()
    val requestedPages = mutableListOf<Int>()
    var available = true

    override fun isAvailable(): Boolean = available

    override fun requestPayment(purchase: Purchase) {
        requested += purchase
        storedAtRequest += runBlocking { purchaseRepository.find(purchase.id) }
    }

    override fun requestOrders(page: Int) {
        requestedPages += page
    }
}
