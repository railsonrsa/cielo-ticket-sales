package dev.railson.ticketsales.feature.catalog.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import dev.railson.ticketsales.feature.catalog.presentation.catalog.CatalogScreen
import kotlinx.serialization.Serializable

@Serializable
data object CatalogRoute

fun NavGraphBuilder.catalogScreen(onOpenCart: () -> Unit) {
    composable<CatalogRoute> { CatalogScreen(onOpenCart = onOpenCart) }
}
