package dev.railson.ticketsales.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyTest {

    @Test
    fun `unit price times quantity gives the line amount`() {
        val unitPrice = Money(cents = 4_550)

        assertThat(unitPrice * 3).isEqualTo(Money(cents = 13_650))
    }

    @Test
    fun `adding two amounts sums their cents`() {
        assertThat(Money(cents = 13_650) + Money(cents = 8_000)).isEqualTo(Money(cents = 21_650))
    }

    @Test
    fun `summing a list of amounts gives the total`() {
        val amounts = listOf(Money(cents = 13_650), Money(cents = 8_000), Money(cents = 350))

        assertThat(amounts.sum()).isEqualTo(Money(cents = 22_000))
    }

    @Test
    fun `summing no amounts gives zero`() {
        assertThat(emptyList<Money>().sum()).isEqualTo(Money(cents = 0))
    }

    @Test
    fun `an amount can never be negative`() {
        assertThrows(IllegalArgumentException::class.java) { Money(cents = -1) }
    }
}
