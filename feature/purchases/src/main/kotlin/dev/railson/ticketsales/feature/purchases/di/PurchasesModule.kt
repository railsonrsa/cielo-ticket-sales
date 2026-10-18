package dev.railson.ticketsales.feature.purchases.di

import dev.railson.ticketsales.core.domain.usecase.payment.AbandonPurchaseUseCase
import dev.railson.ticketsales.core.domain.usecase.payment.AcknowledgeSoldOutUseCase
import dev.railson.ticketsales.core.domain.usecase.purchase.ObservePurchaseUseCase
import dev.railson.ticketsales.core.domain.usecase.purchase.ObservePurchasesUseCase
import dev.railson.ticketsales.core.domain.usecase.purchase.ObserveSoldOutNoticesUseCase
import dev.railson.ticketsales.core.domain.usecase.purchase.ObserveTicketsUseCase
import dev.railson.ticketsales.feature.purchases.presentation.purchases.PurchasesViewModel
import dev.railson.ticketsales.feature.purchases.presentation.receipt.ReceiptViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val purchasesModule = module {
    factoryOf(::ObservePurchasesUseCase)
    factoryOf(::ObservePurchaseUseCase)
    factoryOf(::ObserveTicketsUseCase)
    factoryOf(::AbandonPurchaseUseCase)
    factoryOf(::ObserveSoldOutNoticesUseCase)
    factoryOf(::AcknowledgeSoldOutUseCase)
    viewModelOf(::PurchasesViewModel)
    viewModel { params -> ReceiptViewModel(purchaseId = params.get(), observePurchase = get(), observeTickets = get()) }
}
