package dev.railson.ticketsales.core.domain.usecase.cart

import dev.railson.ticketsales.core.domain.repository.CartRepository
import dev.railson.ticketsales.core.domain.repository.EventRepository
import kotlinx.coroutines.flow.first

/** Mais um ingresso do Event no Cart, sem passar da Capacity atual. */
class AddToCartUseCase(
    private val cartRepository: CartRepository,
    private val eventRepository: EventRepository,
) {

    suspend operator fun invoke(eventId: String): CartChangeResult {
        val event = eventRepository.observeEvents().first().single { it.id == eventId }
        val quantity = (cartRepository.observeQuantities().first()[eventId] ?: 0) + 1
        if (quantity > event.capacity) return CartChangeResult.ExceedsCapacity
        cartRepository.setQuantity(eventId, quantity)
        return CartChangeResult.Applied
    }
}
