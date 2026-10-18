package dev.railson.ticketsales.core.domain.repository

import dev.railson.ticketsales.core.domain.model.CieloOrder
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.model.Ticket
import kotlinx.coroutines.flow.Flow

/** As Purchases em disco. As regras de quando cada mudança vale ficam nos use cases. */
interface PurchaseRepository {

    suspend fun create(purchase: Purchase)

    suspend fun find(id: String): Purchase?

    fun observe(id: String): Flow<Purchase?>

    suspend fun findAwaitingPayment(): Purchase?

    fun observeAwaitingPayment(): Flow<Purchase?>

    fun observeTickets(purchaseId: String): Flow<List<Ticket>>

    /**
     * Aprovação atômica: estado → `Approved`, a [cieloOrder] gravada, um Ticket por unidade, a Capacity de cada
     * Event decrementada e o Cart esvaziado. Ou acontece tudo, ou nada.
     */
    suspend fun approve(purchaseId: String, cieloOrder: CieloOrder)

    suspend fun fail(purchaseId: String, reason: FailureReason, cieloOrder: CieloOrder? = null, pendingRefund: Boolean = false)
}
