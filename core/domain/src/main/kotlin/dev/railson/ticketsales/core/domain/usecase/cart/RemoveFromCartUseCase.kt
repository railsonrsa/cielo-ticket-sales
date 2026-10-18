package dev.railson.ticketsales.core.domain.usecase.cart

import dev.railson.ticketsales.core.domain.repository.CartRepository

/** Tira o Event do Cart, qualquer que seja a quantidade. */
class RemoveFromCartUseCase(private val cartRepository: CartRepository) {

    suspend operator fun invoke(eventId: String) {
        cartRepository.remove(eventId)
    }
}
