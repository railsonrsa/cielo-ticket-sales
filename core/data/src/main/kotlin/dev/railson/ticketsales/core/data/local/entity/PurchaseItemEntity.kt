package dev.railson.ticketsales.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Foto do Event no momento do "Pagar": nome e preço não acompanham mudanças posteriores. */
@Entity(
    tableName = "purchase_item",
    primaryKeys = ["purchase_id", "event_id"],
    foreignKeys = [
        ForeignKey(entity = PurchaseEntity::class, parentColumns = ["id"], childColumns = ["purchase_id"]),
        ForeignKey(entity = EventEntity::class, parentColumns = ["id"], childColumns = ["event_id"]),
    ],
    indices = [Index("event_id")],
)
internal data class PurchaseItemEntity(
    @ColumnInfo(name = "purchase_id") val purchaseId: String,
    @ColumnInfo(name = "event_id") val eventId: String,
    // A ordem das linhas é a ordem do Cart no "Pagar".
    val position: Int,
    @ColumnInfo(name = "event_name") val eventName: String,
    val quantity: Int,
    @ColumnInfo(name = "unit_price_cents") val unitPriceCents: Long,
)
