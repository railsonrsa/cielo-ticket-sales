package dev.railson.ticketsales.core.domain.model

/** Um Event válido; o teste muda só o que importa para ele. */
fun anEvent(
    id: String = "event-1",
    name: String = "Show de teste",
    startsAtEpochMillis: Long = 1_793_000_000_000,
    venue: String = "Teatro Municipal",
    unitPrice: Money = Money(cents = 5_000),
    capacity: Int = 100,
) = Event(id, name, startsAtEpochMillis, venue, unitPrice, capacity)
