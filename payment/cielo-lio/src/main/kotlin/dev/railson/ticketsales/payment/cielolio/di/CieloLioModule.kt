package dev.railson.ticketsales.payment.cielolio.di

import dev.railson.ticketsales.core.domain.terminal.PaymentTerminal
import dev.railson.ticketsales.core.domain.usecase.payment.HandlePaymentOutcomeUseCase
import dev.railson.ticketsales.core.domain.usecase.payment.ReconcilePurchaseUseCase
import dev.railson.ticketsales.payment.cielolio.CieloLioTerminal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val cieloLioModule = module {
    single<PaymentTerminal> { CieloLioTerminal(androidContext()) }
    factoryOf(::HandlePaymentOutcomeUseCase)
    // Single: a busca da Reconciliation vive em memória e é a mesma para as telas e para o callback.
    single { ReconcilePurchaseUseCase(get(), get(), CoroutineScope(SupervisorJob() + Dispatchers.Default)) }
}
