package dev.railson.ticketsales.core.domain.model

/** A seleção que o cliente monta antes de pagar, com o preço atual de cada Event. Não reserva nada. */
data class Cart(val items: List<CartItem>) {

    val total: Money get() = items.map(CartItem::subtotal).sum()

    val hasCapacityConflict: Boolean get() = items.any { it.capacityConflict != null }
}

/** Uma linha do Cart: um Event e quantos ingressos dele. */
data class CartItem(val event: Event, val quantity: Int) {

    val subtotal: Money get() = event.unitPrice * quantity

    val capacityConflict: CapacityConflict?
        get() = when {
            event.capacity == 0 -> CapacityConflict.SoldOut
            quantity > event.capacity -> CapacityConflict.OnlyLeft(available = event.capacity)
            else -> null
        }
}
