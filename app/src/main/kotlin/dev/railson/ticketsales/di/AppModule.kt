package dev.railson.ticketsales.di

import dev.railson.ticketsales.core.data.di.dataModule
import dev.railson.ticketsales.feature.catalog.di.catalogModule
import dev.railson.ticketsales.feature.checkout.di.checkoutModule
import dev.railson.ticketsales.feature.purchases.di.purchasesModule
import dev.railson.ticketsales.payment.fake.di.fakePaymentModule
import org.koin.dsl.module

val appModule = module {
    includes(dataModule, fakePaymentModule, catalogModule, checkoutModule, purchasesModule)
}
