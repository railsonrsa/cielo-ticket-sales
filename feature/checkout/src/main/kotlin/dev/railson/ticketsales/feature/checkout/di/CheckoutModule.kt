package dev.railson.ticketsales.feature.checkout.di

import dev.railson.ticketsales.core.domain.usecase.cart.ChangeCartQuantityUseCase
import dev.railson.ticketsales.core.domain.usecase.cart.ObserveCartUseCase
import dev.railson.ticketsales.core.domain.usecase.cart.RemoveFromCartUseCase
import dev.railson.ticketsales.feature.checkout.presentation.cart.CartViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val checkoutModule = module {
    factoryOf(::ObserveCartUseCase)
    factoryOf(::ChangeCartQuantityUseCase)
    factoryOf(::RemoveFromCartUseCase)
    viewModelOf(::CartViewModel)
}
