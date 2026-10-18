package dev.railson.ticketsales.core.domain.model

/** O direito de entrar num Event, emitido só a partir de uma Purchase aprovada; um por unidade. */
data class Ticket(
    val id: String,
    val purchaseId: String,
    val eventId: String,
)
