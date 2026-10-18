package dev.railson.ticketsales.core.domain.usecase.cart

import dev.railson.ticketsales.core.domain.repository.CartRepository
import dev.railson.ticketsales.core.domain.repository.EventRepository
import kotlinx.coroutines.flow.first

/**
 * Troca a quantidade de um Event no Cart, sem passar da Capacity atual; é também o "Ajustar para N" de um
 * item em conflito. Zerar não é trocar a quantidade: é o [RemoveFromCartUseCase].
 */
class ChangeCartQuantityUseCase(
    private val cartRepository: CartRepository,
    private val eventRepository: EventRepository,
) {

    suspend operator fun invoke(eventId: String, quantity: Int): CartChangeResult {
        require(quantity >= 1) { "A cart item needs at least one ticket: $quantity" }
        val event = eventRepository.observeEvents().first().single { it.id == eventId }
        if (quantity > event.capacity) return CartChangeResult.ExceedsCapacity
        cartRepository.setQuantity(eventId, quantity)
        return CartChangeResult.Applied
    }
}
