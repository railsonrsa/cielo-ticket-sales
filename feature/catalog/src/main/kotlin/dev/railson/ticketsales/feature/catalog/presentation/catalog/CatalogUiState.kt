package dev.railson.ticketsales.feature.catalog.presentation.catalog

import dev.railson.ticketsales.core.domain.model.Event

internal data class CatalogUiState(
    val isLoading: Boolean = true,
    val events: List<Event> = emptyList(),
    /** Quantos ingressos de cada Event já estão no Cart, pelo id do Event. */
    val cartQuantities: Map<String, Int> = emptyMap(),
) {

    val cartTicketCount: Int get() = cartQuantities.values.sum()

    /** A tela limita o Cart à Capacity atual. */
    fun canAdd(event: Event): Boolean = (cartQuantities[event.id] ?: 0) < event.capacity
}
