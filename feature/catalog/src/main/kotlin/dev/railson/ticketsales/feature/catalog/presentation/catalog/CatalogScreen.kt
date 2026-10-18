package dev.railson.ticketsales.feature.catalog.presentation.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun CatalogScreen(viewModel: CatalogViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CatalogContent(uiState = uiState)
}
