package dev.railson.ticketsales.feature.catalog.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.railson.ticketsales.core.domain.usecase.cart.AddToCartUseCase
import dev.railson.ticketsales.core.domain.usecase.cart.ObserveCartUseCase
import dev.railson.ticketsales.core.domain.usecase.catalog.ObserveEventsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class CatalogViewModel(
    observeEvents: ObserveEventsUseCase,
    observeCart: ObserveCartUseCase,
    private val addToCart: AddToCartUseCase,
) : ViewModel() {

    val uiState: StateFlow<CatalogUiState> = combine(observeEvents(), observeCart()) { events, cart ->
        CatalogUiState(
            isLoading = false,
            events = events,
            cartQuantities = cart.items.associate { it.event.id to it.quantity },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogUiState())

    fun onAction(action: CatalogAction) {
        when (action) {
            is CatalogAction.AddToCart -> viewModelScope.launch { addToCart(action.eventId) }
        }
    }
}
