package dev.railson.ticketsales.core.domain.model

import org.junit.Assert.assertThrows
import org.junit.Test

class EventTest {

    @Test
    fun `capacity can never be negative`() {
        assertThrows(IllegalArgumentException::class.java) {
            Event(
                id = "show-1",
                name = "Show",
                startsAtEpochMillis = 0,
                venue = "Teatro",
                unitPrice = Money(cents = 5_000),
                capacity = -1,
            )
        }
    }
}
