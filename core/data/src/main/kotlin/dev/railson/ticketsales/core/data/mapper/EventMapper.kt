package dev.railson.ticketsales.core.data.mapper

import dev.railson.ticketsales.core.data.local.entity.EventEntity
import dev.railson.ticketsales.core.domain.model.Event
import dev.railson.ticketsales.core.domain.model.Money

internal fun EventEntity.toDomain() = Event(
    id = id,
    name = name,
    startsAtEpochMillis = startsAtEpochMillis,
    venue = venue,
    unitPrice = Money(cents = unitPriceCents),
    capacity = capacity,
)
