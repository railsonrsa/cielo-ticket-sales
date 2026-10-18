package dev.railson.ticketsales.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable

/** Destino provisório até a primeira tela (Catálogo, #13) ocupar o início do grafo. */
@Serializable
internal data object StartRoute

@Composable
internal fun AppNavHost() {
    val navController = rememberNavController()
    Surface(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = StartRoute) {
            composable<StartRoute> { }
        }
    }
}
