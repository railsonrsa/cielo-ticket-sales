package dev.railson.ticketsales.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dev.railson.ticketsales.feature.catalog.presentation.navigation.CatalogRoute
import dev.railson.ticketsales.feature.catalog.presentation.navigation.catalogScreen

@Composable
internal fun AppNavHost() {
    val navController = rememberNavController()
    Surface(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = CatalogRoute) {
            catalogScreen()
        }
    }
}
