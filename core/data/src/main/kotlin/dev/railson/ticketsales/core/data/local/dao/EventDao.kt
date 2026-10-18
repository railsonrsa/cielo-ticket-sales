package dev.railson.ticketsales.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import dev.railson.ticketsales.core.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface EventDao {

    @Query("SELECT * FROM event")
    fun observeAll(): Flow<List<EventEntity>>
}
