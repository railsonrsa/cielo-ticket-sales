package dev.railson.ticketsales.core.domain.usecase.cart

import dev.railson.ticketsales.core.domain.model.Cart
import dev.railson.ticketsales.core.domain.model.CartItem
import dev.railson.ticketsales.core.domain.repository.CartRepository
import dev.railson.ticketsales.core.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** O Cart com o preço e a Capacity atuais de cada Event. */
class ObserveCartUseCase(
    private val cartRepository: CartRepository,
    private val eventRepository: EventRepository,
) {

    operator fun invoke(): Flow<Cart> =
        combine(cartRepository.observeQuantities(), eventRepository.observeEvents()) { quantities, events ->
            val eventsById = events.associateBy { it.id }
            Cart(quantities.mapNotNull { (eventId, quantity) -> eventsById[eventId]?.let { CartItem(it, quantity) } })
        }
}
