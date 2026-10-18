package dev.railson.ticketsales.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * O [status] e o [failureReason] vão como texto (o nome no domínio), não como ordinal: reordenar o enum não
 * pode mudar o significado de uma linha gravada. Os campos da Cielo Order só existem depois do callback.
 * [extraOrderIds] são os `orderId`s pagos a mais, separados por vírgula (a Cielo usa UUIDs).
 */
@Entity(tableName = "purchase")
internal data class PurchaseEntity(
    @PrimaryKey val id: String,
    val status: String,
    @ColumnInfo(name = "failure_reason") val failureReason: String?,
    @ColumnInfo(name = "total_cents") val totalCents: Long,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "order_id") val orderId: String?,
    @ColumnInfo(name = "authorized_amount_cents") val authorizedAmountCents: Long?,
    @ColumnInfo(name = "cielo_code") val cieloCode: String?,
    @ColumnInfo(name = "auth_code") val authCode: String?,
    @ColumnInfo(name = "pending_refund") val pendingRefund: Boolean,
    @ColumnInfo(name = "extra_order_ids", defaultValue = "") val extraOrderIds: String,
    @ColumnInfo(name = "inconclusive_checks", defaultValue = "0") val inconclusiveChecks: Int,
)

internal data class PurchaseWithItems(
    @Embedded val purchase: PurchaseEntity,
    @Relation(parentColumn = "id", entityColumn = "purchase_id")
    val items: List<PurchaseItemEntity>,
)
