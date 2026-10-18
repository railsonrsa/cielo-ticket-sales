package dev.railson.ticketsales.core.domain.repository

import dev.railson.ticketsales.core.domain.model.CieloOrder
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.model.Ticket
import kotlinx.coroutines.flow.Flow

/** As Purchases em disco. As regras de quando cada mudança vale ficam nos use cases. */
interface PurchaseRepository {

    suspend fun create(purchase: Purchase)

    suspend fun find(id: String): Purchase?

    fun observe(id: String): Flow<Purchase?>

    /** Todas as Purchases, em qualquer estado e em qualquer ordem: quem filtra e ordena é o use case. */
    fun observeAll(): Flow<List<Purchase>>

    suspend fun findAwaitingPayment(): Purchase?

    fun observeAwaitingPayment(): Flow<Purchase?>

    fun observeTickets(purchaseId: String): Flow<List<Ticket>>

    /**
     * Aprovação atômica: estado → `Approved`, a [cieloOrder] gravada, um Ticket por unidade, a Capacity de cada
     * Event decrementada e o Cart esvaziado. Ou acontece tudo, ou nada. [extraOrderIds] (Orders pagas a mais)
     * ficam gravados com a marca Pending Refund.
     * A aprovação [late] (`Failed` → `Approved`) não mexe no Cart e fica esperando o aceite do Operator.
     *
     * Devolve `false`, sem mudar nada, se faltar Capacity para algum item: a Capacity nunca fica abaixo de 0.
     */
    suspend fun approve(
        purchaseId: String,
        cieloOrder: CieloOrder,
        extraOrderIds: List<String> = emptyList(),
        late: Boolean = false,
    ): Boolean

    suspend fun fail(
        purchaseId: String,
        reason: FailureReason,
        cieloOrder: CieloOrder? = null,
        pendingRefund: Boolean = false,
        extraOrderIds: List<String> = emptyList(),
        needsAcknowledgment: Boolean = false,
    )

    /** O Operator reconheceu o desfecho fora de hora: a marca Pending Refund, se houver, continua. */
    suspend fun acknowledge(purchaseId: String)

    /** Uma Reconciliation terminou sem resposta: a Purchase continua como está, só a contagem sobe. */
    suspend fun recordInconclusiveCheck(purchaseId: String)
}
