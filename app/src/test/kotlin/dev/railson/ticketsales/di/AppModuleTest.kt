package dev.railson.ticketsales.di

import androidx.lifecycle.SavedStateHandle
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

class AppModuleTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `koin graph resolves every definition`() {
        // O SavedStateHandle o Koin entrega em tempo de execução; o id da Purchase vem da rota, via parametersOf.
        appModule.verify(extraTypes = listOf(SavedStateHandle::class, String::class))
    }
}
