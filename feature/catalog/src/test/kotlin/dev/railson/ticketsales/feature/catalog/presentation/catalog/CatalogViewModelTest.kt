package dev.railson.ticketsales.feature.catalog.presentation.catalog

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.railson.ticketsales.core.domain.model.anEvent
import dev.railson.ticketsales.core.domain.repository.FakeEventRepository
import dev.railson.ticketsales.core.domain.usecase.catalog.ObserveEventsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {

    private val repository = FakeEventRepository()

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts loading and then shows the events, the nearest first`() = runTest {
        repository.setEvents(
            listOf(
                anEvent(id = "december", startsAtEpochMillis = 1_796_000_000_000),
                anEvent(id = "november", startsAtEpochMillis = 1_793_000_000_000),
            ),
        )
        val viewModel = CatalogViewModel(ObserveEventsUseCase(repository))

        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(CatalogUiState(isLoading = true))

            val loaded = awaitItem()
            assertThat(loaded.isLoading).isFalse()
            assertThat(loaded.events.map { it.id }).containsExactly("november", "december").inOrder()
        }
    }

    @Test
    fun `a sale that lowers the capacity updates the screen`() = runTest {
        repository.setEvents(listOf(anEvent(id = "show", capacity = 2)))
        val viewModel = CatalogViewModel(ObserveEventsUseCase(repository))

        viewModel.uiState.test {
            skipItems(1)
            assertThat(awaitItem().events.single().capacity).isEqualTo(2)

            repository.setEvents(listOf(anEvent(id = "show", capacity = 0)))

            assertThat(awaitItem().events.single().capacity).isEqualTo(0)
        }
    }

    @Test
    fun `an empty catalog stops loading with no events`() = runTest {
        val viewModel = CatalogViewModel(ObserveEventsUseCase(repository))

        viewModel.uiState.test {
            skipItems(1)
            assertThat(awaitItem()).isEqualTo(CatalogUiState(isLoading = false, events = emptyList()))
        }
    }
}
