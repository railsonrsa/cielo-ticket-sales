package dev.railson.ticketsales.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/** Sem preço: o Cart sempre usa o preço atual do Event. */
@Entity(
    tableName = "cart_item",
    foreignKeys = [ForeignKey(entity = EventEntity::class, parentColumns = ["id"], childColumns = ["event_id"])],
)
internal data class CartItemEntity(
    @PrimaryKey @ColumnInfo(name = "event_id") val eventId: String,
    val quantity: Int,
)
