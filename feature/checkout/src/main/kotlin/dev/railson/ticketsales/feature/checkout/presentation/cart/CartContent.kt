package dev.railson.ticketsales.feature.checkout.presentation.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.railson.ticketsales.core.domain.model.Cart
import dev.railson.ticketsales.core.domain.model.CartItem
import dev.railson.ticketsales.core.domain.model.Event
import dev.railson.ticketsales.core.domain.model.Money
import dev.railson.ticketsales.core.ui.format.formatBrl
import dev.railson.ticketsales.core.ui.theme.TicketSalesTheme
import dev.railson.ticketsales.feature.checkout.R
import dev.railson.ticketsales.feature.checkout.presentation.components.CartItemCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CartContent(
    uiState: CartUiState,
    onAction: (CartAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cart_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cart_back))
                    }
                },
            )
        },
        bottomBar = { if (uiState.cart.items.isNotEmpty()) PayBar(uiState) },
    ) { innerPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(innerPadding)
        when {
            uiState.isLoading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.cart.items.isEmpty() -> Box(contentModifier, contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.cart_empty))
            }
            else -> LazyColumn(
                modifier = contentModifier,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.cart.items, key = { it.event.id }) { item ->
                    CartItemCard(
                        item = item,
                        onChangeQuantity = { quantity -> onAction(CartAction.ChangeQuantity(item.event.id, quantity)) },
                        onRemove = { onAction(CartAction.Remove(item.event.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PayBar(uiState: CartUiState) {
    Surface(tonalElevation = 3.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.cart_total), style = MaterialTheme.typography.titleMedium)
                Text(formatBrl(uiState.cart.total.cents), style = MaterialTheme.typography.titleMedium)
            }
            // O "Pagar" ganha ação na próxima fatia (#15).
            Button(onClick = {}, enabled = uiState.canPay, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cart_pay))
            }
        }
    }
}

@Preview
@Composable
private fun CartContentPreview() {
    TicketSalesTheme {
        CartContent(
            uiState = CartUiState(
                isLoading = false,
                cart = Cart(
                    items = listOf(
                        CartItem(Event("1", "Noite de Choro no Coreto", 1_794_092_400_000, "Praça do Coreto", Money(4_000), 150), 2),
                        CartItem(Event("2", "Voz e Violão: Sessão Intimista", 1_795_910_400_000, "Café do Porto", Money(4_550), 1), 3),
                        CartItem(Event("3", "Stand-up na Praça", 1_796_910_400_000, "Praça Central", Money(3_000), 0), 1),
                    ),
                ),
            ),
            onAction = {},
            onBack = {},
        )
    }
}
