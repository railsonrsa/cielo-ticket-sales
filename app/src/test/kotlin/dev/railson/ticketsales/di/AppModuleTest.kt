package dev.railson.ticketsales.di

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineScope
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

class AppModuleTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `koin graph resolves every definition`() {
        // O SavedStateHandle o Koin entrega em tempo de execução; o id da Purchase vem da rota, via parametersOf.
        // O CoroutineScope da Reconciliation e o Lazy da fake nascem dentro da própria definição.
        appModule.verify(extraTypes = listOf(SavedStateHandle::class, String::class, CoroutineScope::class, Lazy::class))
    }
}
