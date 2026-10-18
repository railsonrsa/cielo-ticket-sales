package dev.railson.ticketsales.feature.catalog.presentation.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.railson.ticketsales.core.domain.model.Event
import dev.railson.ticketsales.core.domain.model.Money
import dev.railson.ticketsales.core.ui.theme.TicketSalesTheme
import dev.railson.ticketsales.feature.catalog.R
import dev.railson.ticketsales.feature.catalog.presentation.components.EventCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CatalogContent(
    uiState: CatalogUiState,
    onAction: (CatalogAction) -> Unit,
    onOpenCart: () -> Unit,
    onOpenPurchases: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.catalog_title)) },
                actions = {
                    IconButton(onClick = onOpenPurchases) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.catalog_open_purchases))
                    }
                    CartButton(ticketCount = uiState.cartTicketCount, onClick = onOpenCart)
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(innerPadding)
        when {
            uiState.isLoading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.events.isEmpty() -> Box(contentModifier, contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.catalog_empty))
            }
            else -> LazyColumn(
                modifier = contentModifier,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.events, key = Event::id) { event ->
                    EventCard(
                        event = event,
                        canAdd = uiState.canAdd(event),
                        onAdd = { onAction(CatalogAction.AddToCart(event.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CartButton(ticketCount: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        BadgedBox(badge = { if (ticketCount > 0) Badge { Text(ticketCount.toString()) } }) {
            Icon(Icons.Filled.ShoppingCart, contentDescription = stringResource(R.string.catalog_open_cart))
        }
    }
}

@Preview
@Composable
private fun CatalogContentPreview() {
    TicketSalesTheme {
        CatalogContent(
            CatalogUiState(
                isLoading = false,
                events = listOf(
                    Event("1", "Noite de Choro no Coreto", 1_794_092_400_000, "Praça do Coreto", Money(4_000), 150),
                    Event("2", "Voz e Violão: Sessão Intimista", 1_795_910_400_000, "Café do Porto", Money(4_550), 0),
                ),
                cartQuantities = mapOf("1" to 2),
            ),
            onAction = {},
            onOpenCart = {},
            onOpenPurchases = {},
        )
    }
}
