package dev.railson.ticketsales.core.domain.model

/** Valor em reais (BRL), guardado em centavos. */
@JvmInline
value class Money(val cents: Long) {

    init {
        require(cents >= 0) { "Money cannot be negative: $cents cents" }
    }

    operator fun plus(other: Money): Money = Money(cents + other.cents)

    operator fun times(quantity: Int): Money = Money(cents * quantity)
}

fun Iterable<Money>.sum(): Money = fold(Money(cents = 0), Money::plus)
