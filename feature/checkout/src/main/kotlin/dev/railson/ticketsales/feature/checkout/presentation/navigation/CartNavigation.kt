package dev.railson.ticketsales.feature.checkout.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.railson.ticketsales.feature.checkout.presentation.cart.CartScreen
import kotlinx.serialization.Serializable

@Serializable
data object CartRoute

fun NavGraphBuilder.cartScreen(onBack: () -> Unit) {
    composable<CartRoute> { CartScreen(onBack = onBack) }
}
