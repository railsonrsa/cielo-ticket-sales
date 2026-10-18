package dev.railson.ticketsales.core.domain.repository

import kotlinx.coroutines.flow.Flow

/** O Cart em disco: só Event e quantidade, sem preço. */
interface CartRepository {

    /** A quantidade de cada Event no Cart, pelo id do Event, na ordem em que entraram. */
    fun observeQuantities(): Flow<Map<String, Int>>

    suspend fun setQuantity(eventId: String, quantity: Int)

    suspend fun remove(eventId: String)
}
