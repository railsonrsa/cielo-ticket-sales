package dev.railson.ticketsales.feature.catalog.presentation.catalog

import dev.railson.ticketsales.core.domain.model.Event

internal data class CatalogUiState(
    val isLoading: Boolean = true,
    val events: List<Event> = emptyList(),
)
