package dev.railson.ticketsales.di

import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

class AppModuleTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `koin graph resolves every definition`() {
        appModule.verify()
    }
}
