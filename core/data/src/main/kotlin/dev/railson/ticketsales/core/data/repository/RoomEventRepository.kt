package dev.railson.ticketsales.core.data.repository

import dev.railson.ticketsales.core.data.local.dao.EventDao
import dev.railson.ticketsales.core.data.mapper.toDomain
import dev.railson.ticketsales.core.domain.model.Event
import dev.railson.ticketsales.core.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomEventRepository(private val eventDao: EventDao) : EventRepository {

    override fun observeEvents(): Flow<List<Event>> =
        eventDao.observeAll().map { entities -> entities.map { it.toDomain() } }
}
