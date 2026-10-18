package dev.railson.ticketsales.core.domain.usecase.purchase

import dev.railson.ticketsales.core.domain.model.Ticket
import dev.railson.ticketsales.core.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.Flow

/** Os Tickets emitidos por uma Purchase, na ordem dos Purchase Items. Vazio enquanto ela não for aprovada. */
class ObserveTicketsUseCase(private val purchaseRepository: PurchaseRepository) {

    operator fun invoke(purchaseId: String): Flow<List<Ticket>> = purchaseRepository.observeTickets(purchaseId)
}
