package dev.railson.ticketsales

import android.app.Application
import dev.railson.ticketsales.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TicketSalesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TicketSalesApplication)
            modules(appModule)
        }
    }
}
