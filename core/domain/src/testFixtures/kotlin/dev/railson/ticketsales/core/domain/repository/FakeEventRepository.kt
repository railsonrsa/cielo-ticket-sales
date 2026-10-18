package dev.railson.ticketsales.core.domain.repository

import dev.railson.ticketsales.core.domain.model.Event
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Repositório em memória: o teste troca a lista e quem observa recebe a nova. */
class FakeEventRepository(events: List<Event> = emptyList()) : EventRepository {

    private val events = MutableStateFlow(events)

    fun setEvents(events: List<Event>) {
        this.events.value = events
    }

    override fun observeEvents(): Flow<List<Event>> = events
}
