package dev.railson.ticketsales.core.domain.repository

import dev.railson.ticketsales.core.domain.model.CieloOrder
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.model.PurchaseStatus
import dev.railson.ticketsales.core.domain.model.Ticket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Purchases em memória. A aprovação mexe nos fakes de Event e de Cart como a transação do Room:
 * confere a Capacity de todos os itens antes de mudar qualquer coisa.
 */
class FakePurchaseRepository(
    private val eventRepository: FakeEventRepository,
    private val cartRepository: FakeCartRepository,
) : PurchaseRepository {

    private val purchases = MutableStateFlow<Map<String, Purchase>>(emptyMap())
    private val tickets = MutableStateFlow<List<Ticket>>(emptyList())
    private var nextTicket = 0

    override suspend fun create(purchase: Purchase) {
        purchases.update { it + (purchase.id to purchase) }
    }

    override suspend fun find(id: String): Purchase? = purchases.value[id]

    override fun observe(id: String): Flow<Purchase?> = purchases.map { it[id] }

    override suspend fun findAwaitingPayment(): Purchase? = observeAwaitingPayment().first()

    override fun observeAwaitingPayment(): Flow<Purchase?> =
        purchases.map { all -> all.values.singleOrNull { it.status == PurchaseStatus.AwaitingPayment } }

    override fun observeTickets(purchaseId: String): Flow<List<Ticket>> =
        tickets.map { all -> all.filter { it.purchaseId == purchaseId } }

    override suspend fun approve(purchaseId: String, cieloOrder: CieloOrder) {
        val purchase = purchases.value.getValue(purchaseId)
        val events = eventRepository.observeEvents().first()
        val soldByEvent = purchase.items.groupBy { it.eventId }.mapValues { (_, items) -> items.sumOf { it.quantity } }
        check(soldByEvent.all { (eventId, sold) -> events.single { it.id == eventId }.capacity >= sold }) {
            "Not enough capacity to approve $purchaseId"
        }

        eventRepository.setEvents(events.map { it.copy(capacity = it.capacity - (soldByEvent[it.id] ?: 0)) })
        cartRepository.observeQuantities().first().keys.forEach { cartRepository.remove(it) }
        tickets.update { all ->
            all + purchase.items.flatMap { item -> List(item.quantity) { Ticket("ticket-${nextTicket++}", purchaseId, item.eventId) } }
        }
        purchases.update { it + (purchaseId to purchase.copy(status = PurchaseStatus.Approved, cieloOrder = cieloOrder)) }
    }

    override suspend fun fail(purchaseId: String, reason: FailureReason, cieloOrder: CieloOrder?, pendingRefund: Boolean) {
        purchases.update { all ->
            val purchase = all.getValue(purchaseId)
            all + (purchaseId to purchase.copy(status = PurchaseStatus.Failed(reason), cieloOrder = cieloOrder, pendingRefund = pendingRefund))
        }
    }
}
