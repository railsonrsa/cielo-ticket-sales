package dev.railson.ticketsales.payment.fake.di

import dev.railson.ticketsales.core.domain.terminal.PaymentTerminal
import dev.railson.ticketsales.core.domain.usecase.payment.HandlePaymentOutcomeUseCase
import dev.railson.ticketsales.payment.fake.FakePaymentTerminal
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val fakePaymentModule = module {
    single<PaymentTerminal> { FakePaymentTerminal(androidContext()) }
    factoryOf(::HandlePaymentOutcomeUseCase)
}
