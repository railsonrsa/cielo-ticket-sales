package dev.railson.ticketsales.payment.cielolio.di

import dev.railson.ticketsales.core.domain.terminal.PaymentTerminal
import dev.railson.ticketsales.core.domain.usecase.payment.HandlePaymentOutcomeUseCase
import dev.railson.ticketsales.payment.cielolio.CieloLioTerminal
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val cieloLioModule = module {
    single<PaymentTerminal> { CieloLioTerminal(androidContext()) }
    factoryOf(::HandlePaymentOutcomeUseCase)
}
