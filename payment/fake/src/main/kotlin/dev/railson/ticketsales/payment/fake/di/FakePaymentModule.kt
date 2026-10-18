package dev.railson.ticketsales.payment.fake.di

import dev.railson.ticketsales.core.domain.terminal.PaymentTerminal
import dev.railson.ticketsales.core.domain.usecase.payment.HandlePaymentOutcomeUseCase
import dev.railson.ticketsales.core.domain.usecase.payment.ReconcilePurchaseUseCase
import dev.railson.ticketsales.payment.fake.FakeCieloOrders
import dev.railson.ticketsales.payment.fake.FakePaymentTerminal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val fakePaymentModule = module {
    single { FakeCieloOrders() }
    // A Reconciliation depende da maquininha, e a fake responde à Reconciliation: um dos lados precisa ser lazy.
    single<PaymentTerminal> {
        FakePaymentTerminal(androidContext(), get(), lazy { get<ReconcilePurchaseUseCase>() }, CoroutineScope(SupervisorJob() + Dispatchers.Default))
    }
    factoryOf(::HandlePaymentOutcomeUseCase)
    // Single: a busca da Reconciliation vive em memória e é a mesma para as telas e para a maquininha.
    single { ReconcilePurchaseUseCase(get(), get(), CoroutineScope(SupervisorJob() + Dispatchers.Default)) }
}
