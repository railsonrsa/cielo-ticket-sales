package dev.railson.ticketsales.payment.fake

import dev.railson.ticketsales.core.domain.model.CieloOrder
import dev.railson.ticketsales.core.domain.model.Money
import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.terminal.ListedOrder
import dev.railson.ticketsales.core.domain.terminal.OrdersPage
import dev.railson.ticketsales.core.domain.terminal.PaymentOutcome
import java.util.UUID

/**
 * A lista de Orders da "Cielo" simulada, para a Reconciliation da demo. Como a de verdade, toda cobrança disparada
 * vira uma Order `ENTERED` (mesmo se o Operator voltar sem responder), e a aprovação a deixa paga.
 * Fica só em memória: depois de o processo morrer, a lista volta vazia.
 */
internal class FakeCieloOrders(private val now: () -> Long = System::currentTimeMillis) {

    private val orders = mutableListOf<ListedOrder>()

    @Synchronized
    fun entered(purchase: Purchase) {
        orders += ListedOrder(
            reference = purchase.id,
            createdAtEpochMillis = now(),
            cieloOrder = CieloOrder(UUID.randomUUID().toString(), authorizedAmount = Money(0), cieloCode = "", authCode = ""),
        )
    }

    /** Uma cobrança autorizada passa a constar como paga na Order do `reference`. */
    @Synchronized
    fun record(outcome: PaymentOutcome) {
        if (outcome !is PaymentOutcome.Paid) return
        val index = orders.indexOfLast { it.reference == outcome.reference }
        if (index >= 0) orders[index] = orders[index].copy(cieloOrder = outcome.cieloOrder)
    }

    /** Da mais antiga para a mais nova, de 5 em 5, como no emulador. */
    @Synchronized
    fun page(number: Int): OrdersPage = OrdersPage(
        number = number,
        totalPages = (orders.size + PAGE_SIZE - 1) / PAGE_SIZE,
        orders = orders.drop((number - 1) * PAGE_SIZE).take(PAGE_SIZE),
    )

    private companion object {
        const val PAGE_SIZE = 5
    }
}
