package dev.railson.ticketsales.core.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.railson.ticketsales.core.data.local.dao.EventDao
import dev.railson.ticketsales.core.data.local.entity.EventEntity
import dev.railson.ticketsales.core.data.seed.EventSeedCallback

/**
 * Sem `fallbackToDestructiveMigration`: apagaria Purchases pendentes e marcas de Pending Refund.
 * Toda mudança de esquema ganha uma migração de verdade.
 */
@Database(entities = [EventEntity::class], version = 1, exportSchema = true)
internal abstract class TicketSalesDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao

    companion object {
        const val NAME = "ticket-sales.db"

        fun build(context: Context, name: String = NAME): TicketSalesDatabase =
            Room.databaseBuilder(context, TicketSalesDatabase::class.java, name)
                .addCallback(EventSeedCallback(context.assets))
                .build()
    }
}
