package dev.railson.ticketsales.feature.catalog.presentation.catalog

internal sealed interface CatalogAction {
    data class AddToCart(val eventId: String) : CatalogAction
}
