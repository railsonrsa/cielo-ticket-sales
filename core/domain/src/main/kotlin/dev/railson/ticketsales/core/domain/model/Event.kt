package dev.railson.ticketsales.core.domain.model

/** Um acontecimento local à venda. A [capacity] é quantos ingressos ainda podem ser vendidos. */
data class Event(
    val id: String,
    val name: String,
    val startsAtEpochMillis: Long,
    val venue: String,
    val unitPrice: Money,
    val capacity: Int,
) {

    init {
        require(capacity >= 0) { "Capacity cannot be negative: $capacity" }
    }
}
