package dev.railson.ticketsales.core.data.local.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.railson.ticketsales.core.data.local.dao.CartItemDao
import dev.railson.ticketsales.core.data.local.dao.EventDao
import dev.railson.ticketsales.core.data.local.entity.CartItemEntity
import dev.railson.ticketsales.core.data.local.entity.EventEntity
import dev.railson.ticketsales.core.data.seed.EventSeedCallback

/**
 * Sem `fallbackToDestructiveMigration`: apagaria Purchases pendentes e marcas de Pending Refund.
 * Toda mudança de esquema ganha uma migração de verdade.
 */
@Database(
    entities = [EventEntity::class, CartItemEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
internal abstract class TicketSalesDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao

    abstract fun cartItemDao(): CartItemDao

    companion object {
        const val NAME = "ticket-sales.db"

        fun build(context: Context, name: String = NAME): TicketSalesDatabase =
            Room.databaseBuilder(context, TicketSalesDatabase::class.java, name)
                .addCallback(EventSeedCallback(context.assets))
                .build()
    }
}
