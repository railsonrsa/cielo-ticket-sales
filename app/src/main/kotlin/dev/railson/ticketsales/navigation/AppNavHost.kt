package dev.railson.ticketsales.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dev.railson.ticketsales.feature.catalog.presentation.navigation.CatalogRoute
import dev.railson.ticketsales.feature.catalog.presentation.navigation.catalogScreen
import dev.railson.ticketsales.feature.checkout.presentation.navigation.CartRoute
import dev.railson.ticketsales.feature.checkout.presentation.navigation.PurchaseResultRoute
import dev.railson.ticketsales.feature.checkout.presentation.navigation.cartScreen
import dev.railson.ticketsales.feature.checkout.presentation.navigation.purchaseResultScreen
import dev.railson.ticketsales.feature.purchases.presentation.navigation.PurchasesRoute
import dev.railson.ticketsales.feature.purchases.presentation.navigation.ReceiptRoute
import dev.railson.ticketsales.feature.purchases.presentation.navigation.purchasesScreen
import dev.railson.ticketsales.feature.purchases.presentation.navigation.receiptScreen

@Composable
internal fun AppNavHost() {
    val navController = rememberNavController()
    Surface(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = CatalogRoute) {
            catalogScreen(
                onOpenCart = { navController.navigate(CartRoute) },
                onOpenPurchases = { navController.navigate(PurchasesRoute) },
            )
            cartScreen(
                onBack = { navController.popBackStack() },
                onPurchaseApproved = { purchaseId ->
                    // O Cart já foi esvaziado: o resultado substitui a tela dele.
                    navController.navigate(PurchaseResultRoute(purchaseId)) { popUpTo(CatalogRoute) }
                },
            )
            purchaseResultScreen(
                onNewSale = { navController.popBackStack(CatalogRoute, inclusive = false) },
                onOpenReceipt = { purchaseId ->
                    // Voltar do comprovante leva ao catálogo, como o "Nova venda": o resultado já cumpriu o papel.
                    navController.navigate(ReceiptRoute(purchaseId)) { popUpTo(CatalogRoute) }
                },
            )
            purchasesScreen(
                onBack = { navController.popBackStack() },
                onOpenReceipt = { purchaseId -> navController.navigate(ReceiptRoute(purchaseId)) },
            )
            receiptScreen(onBack = { navController.popBackStack() })
        }
    }
}
