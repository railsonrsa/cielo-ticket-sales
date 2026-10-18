package dev.railson.ticketsales.core.domain.repository

import dev.railson.ticketsales.core.domain.model.Event
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun observeEvents(): Flow<List<Event>>
}
