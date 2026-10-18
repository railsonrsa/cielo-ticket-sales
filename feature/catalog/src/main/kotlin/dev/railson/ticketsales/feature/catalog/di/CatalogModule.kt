package dev.railson.ticketsales.feature.catalog.di

import dev.railson.ticketsales.core.domain.usecase.cart.AddToCartUseCase
import dev.railson.ticketsales.core.domain.usecase.cart.ObserveCartUseCase
import dev.railson.ticketsales.core.domain.usecase.catalog.ObserveEventsUseCase
import dev.railson.ticketsales.feature.catalog.presentation.catalog.CatalogViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val catalogModule = module {
    factoryOf(::ObserveEventsUseCase)
    factoryOf(::ObserveCartUseCase)
    factoryOf(::AddToCartUseCase)
    viewModelOf(::CatalogViewModel)
}
