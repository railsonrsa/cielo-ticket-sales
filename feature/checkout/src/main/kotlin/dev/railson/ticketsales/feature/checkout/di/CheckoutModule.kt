package dev.railson.ticketsales.feature.checkout.di

import dev.railson.ticketsales.core.domain.usecase.cart.ChangeCartQuantityUseCase
import dev.railson.ticketsales.core.domain.usecase.cart.ObserveCartUseCase
import dev.railson.ticketsales.core.domain.usecase.cart.RemoveFromCartUseCase
import dev.railson.ticketsales.core.domain.usecase.payment.AbandonPurchaseUseCase
import dev.railson.ticketsales.core.domain.usecase.payment.StartPurchaseUseCase
import dev.railson.ticketsales.core.domain.usecase.purchase.ObserveAwaitingPaymentUseCase
import dev.railson.ticketsales.core.domain.usecase.purchase.ObservePurchaseUseCase
import dev.railson.ticketsales.feature.checkout.presentation.cart.CartViewModel
import dev.railson.ticketsales.feature.checkout.presentation.result.PurchaseResultViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val checkoutModule = module {
    factoryOf(::ObserveCartUseCase)
    factoryOf(::ChangeCartQuantityUseCase)
    factoryOf(::RemoveFromCartUseCase)
    factoryOf(::ObserveAwaitingPaymentUseCase)
    factoryOf(::ObservePurchaseUseCase)
    // Single: o Mutex da trava "uma AwaitingPayment por vez" precisa ser o mesmo para todo o app.
    single { StartPurchaseUseCase(get(), get(), get()) }
    factoryOf(::AbandonPurchaseUseCase)
    viewModelOf(::CartViewModel)
    viewModel { params -> PurchaseResultViewModel(purchaseId = params.get(), observePurchase = get()) }
}
