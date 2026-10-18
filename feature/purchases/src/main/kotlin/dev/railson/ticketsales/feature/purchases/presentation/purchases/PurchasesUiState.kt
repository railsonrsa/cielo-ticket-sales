package dev.railson.ticketsales.feature.purchases.presentation.purchases

import dev.railson.ticketsales.core.domain.model.Purchase

/** As [purchases] já chegam filtradas e na ordem de "Minhas compras". */
internal data class PurchasesUiState(
    val isLoading: Boolean = true,
    val purchases: List<Purchase> = emptyList(),
)
