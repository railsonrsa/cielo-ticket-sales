package dev.railson.ticketsales.core.domain.usecase.catalog

import dev.railson.ticketsales.core.domain.model.Event
import dev.railson.ticketsales.core.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Os Events à venda, o mais próximo primeiro. */
class ObserveEventsUseCase(private val eventRepository: EventRepository) {

    operator fun invoke(): Flow<List<Event>> =
        eventRepository.observeEvents().map { events -> events.sortedBy(Event::startsAtEpochMillis) }
}
