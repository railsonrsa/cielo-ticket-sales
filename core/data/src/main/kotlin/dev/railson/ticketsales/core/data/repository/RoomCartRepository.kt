package dev.railson.ticketsales.core.data.repository

import dev.railson.ticketsales.core.data.local.dao.CartItemDao
import dev.railson.ticketsales.core.data.local.entity.CartItemEntity
import dev.railson.ticketsales.core.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomCartRepository(private val cartItemDao: CartItemDao) : CartRepository {

    override fun observeQuantities(): Flow<Map<String, Int>> =
        cartItemDao.observeAll().map { items -> items.associate { it.eventId to it.quantity } }

    override suspend fun setQuantity(eventId: String, quantity: Int) {
        cartItemDao.upsert(CartItemEntity(eventId = eventId, quantity = quantity))
    }

    override suspend fun remove(eventId: String) {
        cartItemDao.delete(eventId)
    }
}
