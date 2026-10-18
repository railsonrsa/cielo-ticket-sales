package dev.railson.ticketsales.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import dev.railson.ticketsales.core.data.local.entity.PurchaseEntity
import dev.railson.ticketsales.core.data.local.entity.PurchaseItemEntity
import dev.railson.ticketsales.core.data.local.entity.PurchaseWithItems
import dev.railson.ticketsales.core.data.local.entity.TicketEntity
import kotlinx.coroutines.flow.Flow

/** Peças da Purchase; quem junta as peças numa transação é o repositório. */
@Dao
internal interface PurchaseDao {

    @Insert
    suspend fun insert(purchase: PurchaseEntity)

    @Insert
    suspend fun insertItems(items: List<PurchaseItemEntity>)

    @Insert
    suspend fun insertTickets(tickets: List<TicketEntity>)

    @Transaction
    @Query("SELECT * FROM purchase WHERE id = :id")
    suspend fun find(id: String): PurchaseWithItems?

    @Transaction
    @Query("SELECT * FROM purchase WHERE id = :id")
    fun observe(id: String): Flow<PurchaseWithItems?>

    @Transaction
    @Query("SELECT * FROM purchase")
    fun observeAll(): Flow<List<PurchaseWithItems>>

    @Transaction
    @Query("SELECT * FROM purchase WHERE status = :status ORDER BY created_at_epoch_millis LIMIT 1")
    suspend fun findByStatus(status: String): PurchaseWithItems?

    @Transaction
    @Query("SELECT * FROM purchase WHERE status = :status ORDER BY created_at_epoch_millis LIMIT 1")
    fun observeByStatus(status: String): Flow<PurchaseWithItems?>

    @Query("SELECT * FROM ticket WHERE purchase_id = :purchaseId ORDER BY rowid")
    fun observeTickets(purchaseId: String): Flow<List<TicketEntity>>

    @Query(
        "UPDATE purchase SET status = :status, failure_reason = :failureReason, order_id = :orderId, " +
            "authorized_amount_cents = :authorizedAmountCents, cielo_code = :cieloCode, auth_code = :authCode, " +
            "pending_refund = :pendingRefund, extra_order_ids = :extraOrderIds, needs_acknowledgment = :needsAcknowledgment " +
            "WHERE id = :id",
    )
    suspend fun updateOutcome(
        id: String,
        status: String,
        failureReason: String?,
        orderId: String?,
        authorizedAmountCents: Long?,
        cieloCode: String?,
        authCode: String?,
        pendingRefund: Boolean,
        extraOrderIds: String,
        needsAcknowledgment: Boolean,
    )

    @Query("UPDATE purchase SET needs_acknowledgment = 0 WHERE id = :id")
    suspend fun clearNeedsAcknowledgment(id: String)

    @Query("UPDATE purchase SET inconclusive_checks = inconclusive_checks + 1 WHERE id = :id")
    suspend fun incrementInconclusiveChecks(id: String)

    /** Devolve 0 quando não há lugar suficiente: a Capacity nunca fica abaixo de 0. */
    @Query("UPDATE event SET capacity = capacity - :quantity WHERE id = :eventId AND capacity >= :quantity")
    suspend fun decrementCapacity(eventId: String, quantity: Int): Int

    @Query("DELETE FROM cart_item")
    suspend fun clearCart()
}
