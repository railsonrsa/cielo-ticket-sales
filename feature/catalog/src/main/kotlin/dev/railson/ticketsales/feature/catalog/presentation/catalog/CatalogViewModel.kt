package dev.railson.ticketsales.feature.catalog.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.railson.ticketsales.core.domain.usecase.catalog.ObserveEventsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class CatalogViewModel(observeEvents: ObserveEventsUseCase) : ViewModel() {

    val uiState: StateFlow<CatalogUiState> = observeEvents()
        .map { events -> CatalogUiState(isLoading = false, events = events) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogUiState())
}
