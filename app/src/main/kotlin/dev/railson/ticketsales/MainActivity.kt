package dev.railson.ticketsales

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.railson.ticketsales.core.ui.theme.TicketSalesTheme
import dev.railson.ticketsales.navigation.AppNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TicketSalesTheme {
                AppNavHost()
            }
        }
    }
}
