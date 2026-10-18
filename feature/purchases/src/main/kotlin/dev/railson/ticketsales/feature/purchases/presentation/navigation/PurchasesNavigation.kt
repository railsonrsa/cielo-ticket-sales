package dev.railson.ticketsales.feature.purchases.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.railson.ticketsales.feature.purchases.presentation.purchases.PurchasesScreen
import dev.railson.ticketsales.feature.purchases.presentation.receipt.ReceiptScreen
import kotlinx.serialization.Serializable

@Serializable
data object PurchasesRoute

@Serializable
data class ReceiptRoute(val purchaseId: String)

fun NavGraphBuilder.purchasesScreen(onBack: () -> Unit, onOpenReceipt: (purchaseId: String) -> Unit) {
    composable<PurchasesRoute> { PurchasesScreen(onBack = onBack, onOpenReceipt = onOpenReceipt) }
}

fun NavGraphBuilder.receiptScreen(onBack: () -> Unit) {
    composable<ReceiptRoute> { entry ->
        ReceiptScreen(purchaseId = entry.toRoute<ReceiptRoute>().purchaseId, onBack = onBack)
    }
}
