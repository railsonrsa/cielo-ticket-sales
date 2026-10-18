package dev.railson.ticketsales.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "event")
internal data class EventEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "starts_at_epoch_millis") val startsAtEpochMillis: Long,
    val venue: String,
    @ColumnInfo(name = "unit_price_cents") val unitPriceCents: Long,
    val capacity: Int,
)
