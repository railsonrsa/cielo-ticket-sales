package dev.railson.ticketsales.core.data.repository

import androidx.room.withTransaction
import dev.railson.ticketsales.core.data.local.database.TicketSalesDatabase
import dev.railson.ticketsales.core.data.local.entity.TicketEntity
import dev.railson.ticketsales.core.data.mapper.APPROVED
import dev.railson.ticketsales.core.data.mapper.AWAITING_PAYMENT
import dev.railson.ticketsales.core.data.mapper.FAILED
import dev.railson.ticketsales.core.data.mapper.itemEntities
import dev.railson.ticketsales.core.data.mapper.toDomain
import dev.railson.ticketsales.core.data.mapper.toEntity
import dev.railson.ticketsales.core.domain.model.CieloOrder
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.model.Ticket
import dev.railson.ticketsales.core.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

internal class RoomPurchaseRepository(
    private val database: TicketSalesDatabase,
    private val newTicketId: () -> String = { UUID.randomUUID().toString() },
) : PurchaseRepository {

    private val purchaseDao = database.purchaseDao()

    override suspend fun create(purchase: Purchase) {
        database.withTransaction {
            purchaseDao.insert(purchase.toEntity())
            purchaseDao.insertItems(purchase.itemEntities())
        }
    }

    override suspend fun find(id: String): Purchase? = purchaseDao.find(id)?.toDomain()

    override fun observe(id: String): Flow<Purchase?> = purchaseDao.observe(id).map { it?.toDomain() }

    override fun observeAll(): Flow<List<Purchase>> =
        purchaseDao.observeAll().map { purchases -> purchases.map { it.toDomain() } }

    override suspend fun findAwaitingPayment(): Purchase? = purchaseDao.findByStatus(AWAITING_PAYMENT)?.toDomain()

    override fun observeAwaitingPayment(): Flow<Purchase?> =
        purchaseDao.observeByStatus(AWAITING_PAYMENT).map { it?.toDomain() }

    override fun observeTickets(purchaseId: String): Flow<List<Ticket>> =
        purchaseDao.observeTickets(purchaseId).map { tickets -> tickets.map { it.toDomain() } }

    override suspend fun approve(purchaseId: String, cieloOrder: CieloOrder) {
        database.withTransaction {
            val purchase = checkNotNull(purchaseDao.find(purchaseId)?.toDomain()) { "Unknown purchase $purchaseId" }
            purchase.items.forEach { item ->
                // Uma exceção aqui desfaz a transação inteira: nada de aprovação parcial.
                check(purchaseDao.decrementCapacity(item.eventId, item.quantity) == 1) {
                    "Not enough capacity in ${item.eventId} to approve $purchaseId"
                }
            }
            purchaseDao.insertTickets(
                purchase.items.flatMap { item -> List(item.quantity) { TicketEntity(newTicketId(), purchaseId, item.eventId) } },
            )
            purchaseDao.clearCart()
            purchaseDao.updateOutcome(
                id = purchaseId,
                status = APPROVED,
                failureReason = null,
                orderId = cieloOrder.orderId,
                authorizedAmountCents = cieloOrder.authorizedAmount.cents,
                cieloCode = cieloOrder.cieloCode,
                authCode = cieloOrder.authCode,
                pendingRefund = false,
            )
        }
    }

    override suspend fun fail(purchaseId: String, reason: FailureReason, cieloOrder: CieloOrder?, pendingRefund: Boolean) {
        purchaseDao.updateOutcome(
            id = purchaseId,
            status = FAILED,
            failureReason = reason.name,
            orderId = cieloOrder?.orderId,
            authorizedAmountCents = cieloOrder?.authorizedAmount?.cents,
            cieloCode = cieloOrder?.cieloCode,
            authCode = cieloOrder?.authCode,
            pendingRefund = pendingRefund,
        )
    }
}
