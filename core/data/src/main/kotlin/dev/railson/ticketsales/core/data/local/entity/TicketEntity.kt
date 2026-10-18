package dev.railson.ticketsales.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ticket",
    foreignKeys = [
        ForeignKey(entity = PurchaseEntity::class, parentColumns = ["id"], childColumns = ["purchase_id"]),
        ForeignKey(entity = EventEntity::class, parentColumns = ["id"], childColumns = ["event_id"]),
    ],
    indices = [Index("purchase_id"), Index("event_id")],
)
internal data class TicketEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "purchase_id") val purchaseId: String,
    @ColumnInfo(name = "event_id") val eventId: String,
)
