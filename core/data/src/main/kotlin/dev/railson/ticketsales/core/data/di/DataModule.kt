package dev.railson.ticketsales.core.data.di

import dev.railson.ticketsales.core.data.local.database.TicketSalesDatabase
import dev.railson.ticketsales.core.data.repository.RoomEventRepository
import dev.railson.ticketsales.core.domain.repository.EventRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val dataModule = module {
    single { TicketSalesDatabase.build(androidContext()) }
    single { get<TicketSalesDatabase>().eventDao() }
    singleOf(::RoomEventRepository) bind EventRepository::class
}
