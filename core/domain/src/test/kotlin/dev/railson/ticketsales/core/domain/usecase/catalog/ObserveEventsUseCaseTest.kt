package dev.railson.ticketsales.core.domain.usecase.catalog

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.railson.ticketsales.core.domain.model.anEvent
import dev.railson.ticketsales.core.domain.repository.FakeEventRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ObserveEventsUseCaseTest {

    @Test
    fun `events come ordered by date, the nearest first`() = runTest {
        val december = anEvent(id = "december", startsAtEpochMillis = 1_796_000_000_000)
        val november = anEvent(id = "november", startsAtEpochMillis = 1_793_000_000_000)
        val observeEvents = ObserveEventsUseCase(FakeEventRepository(listOf(december, november)))

        assertThat(observeEvents().first().map { it.id }).containsExactly("november", "december").inOrder()
    }

    @Test
    fun `a capacity change in the repository reaches the observer`() = runTest {
        val repository = FakeEventRepository(listOf(anEvent(id = "show", capacity = 10)))
        val observeEvents = ObserveEventsUseCase(repository)

        observeEvents().test {
            assertThat(awaitItem().single().capacity).isEqualTo(10)

            repository.setEvents(listOf(anEvent(id = "show", capacity = 7)))

            assertThat(awaitItem().single().capacity).isEqualTo(7)
        }
    }
}
