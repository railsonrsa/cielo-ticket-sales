package dev.railson.ticketsales.core.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Cart em memória, na ordem em que os Events entraram, como o Room. */
class FakeCartRepository(quantities: Map<String, Int> = emptyMap()) : CartRepository {

    private val quantities = MutableStateFlow(quantities)

    override fun observeQuantities(): Flow<Map<String, Int>> = quantities

    override suspend fun setQuantity(eventId: String, quantity: Int) {
        quantities.update { it + (eventId to quantity) }
    }

    override suspend fun remove(eventId: String) {
        quantities.update { it - eventId }
    }
}
