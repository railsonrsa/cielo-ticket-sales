package dev.railson.ticketsales.feature.checkout.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.railson.ticketsales.feature.checkout.presentation.cart.CartScreen
import dev.railson.ticketsales.feature.checkout.presentation.result.PurchaseResultScreen
import kotlinx.serialization.Serializable

@Serializable
data object CartRoute

@Serializable
data class PurchaseResultRoute(val purchaseId: String)

fun NavGraphBuilder.cartScreen(onBack: () -> Unit, onPurchaseApproved: (purchaseId: String) -> Unit) {
    composable<CartRoute> { CartScreen(onBack = onBack, onPurchaseApproved = onPurchaseApproved) }
}

fun NavGraphBuilder.purchaseResultScreen(onNewSale: () -> Unit, onOpenReceipt: (purchaseId: String) -> Unit) {
    composable<PurchaseResultRoute> { entry ->
        PurchaseResultScreen(
            purchaseId = entry.toRoute<PurchaseResultRoute>().purchaseId,
            onNewSale = onNewSale,
            onOpenReceipt = onOpenReceipt,
        )
    }
}
