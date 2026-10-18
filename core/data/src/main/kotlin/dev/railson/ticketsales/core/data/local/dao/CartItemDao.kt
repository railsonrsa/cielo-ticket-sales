package dev.railson.ticketsales.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.railson.ticketsales.core.data.local.entity.CartItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface CartItemDao {

    // O upsert atualiza a linha no lugar, então o rowid guarda a ordem em que o Event entrou no Cart.
    @Query("SELECT * FROM cart_item ORDER BY rowid")
    fun observeAll(): Flow<List<CartItemEntity>>

    @Upsert
    suspend fun upsert(item: CartItemEntity)

    @Query("DELETE FROM cart_item WHERE event_id = :eventId")
    suspend fun delete(eventId: String)
}
