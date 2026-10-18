package dev.railson.ticketsales

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import dev.railson.ticketsales.core.domain.usecase.payment.ReconcilePurchaseUseCase
import dev.railson.ticketsales.core.ui.theme.TicketSalesTheme
import dev.railson.ticketsales.navigation.AppNavHost
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val reconcilePurchase: ReconcilePurchaseUseCase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A abertura a frio reconcilia a Purchase órfã, uma vez por processo. Nunca no resume (ADR 0001): a pessoa
        // pode estar voltando da tela de pagamento da Cielo.
        lifecycleScope.launch { reconcilePurchase.onColdStart() }
        setContent {
            TicketSalesTheme {
                AppNavHost()
            }
        }
    }
}
