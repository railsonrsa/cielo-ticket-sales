package dev.railson.ticketsales.feature.purchases.presentation.receipt

import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.model.Ticket

internal data class ReceiptUiState(
    val purchase: Purchase? = null,
    val tickets: List<Ticket> = emptyList(),
) {

    /** O nome do Event de um Ticket vem do Purchase Item: é o nome do momento da venda. */
    fun eventName(ticket: Ticket): String = purchase?.items?.firstOrNull { it.eventId == ticket.eventId }?.eventName.orEmpty()
}
