package dev.railson.ticketsales.core.domain.terminal

import dev.railson.ticketsales.core.domain.model.CieloOrder
import dev.railson.ticketsales.core.domain.model.Money

/** A lista de Cielo Orders do `lio://orders`, paginada de 5 em 5 e na ordem em que o teste a montou. */
class CieloOrdersStub(private val orders: List<ListedOrder>, private val pageSize: Int = 5) {

    val totalPages: Int get() = (orders.size + pageSize - 1) / pageSize

    fun page(number: Int): OrdersPage =
        OrdersPage(number = number, totalPages = totalPages, orders = orders.drop((number - 1) * pageSize).take(pageSize))
}

/** Uma Cielo Order de outra venda, que só ocupa espaço na lista. */
fun anotherOrder(index: Int, createdAtEpochMillis: Long? = null, paid: Boolean = true) = ListedOrder(
    reference = "other-$index",
    createdAtEpochMillis = createdAtEpochMillis,
    cieloOrder = CieloOrder(
        orderId = "other-order-$index",
        authorizedAmount = Money(if (paid) 1_000 else 0),
        cieloCode = "c$index",
        authCode = "a$index",
    ),
)

/** A Cielo Order de uma Purchase: paga com [authorized], ou só `ENTERED` com zero. */
fun anOrderFor(
    reference: String,
    authorized: Money,
    orderId: String = "order-of-$reference",
    createdAtEpochMillis: Long? = null,
) = ListedOrder(
    reference = reference,
    createdAtEpochMillis = createdAtEpochMillis,
    cieloOrder = CieloOrder(orderId = orderId, authorizedAmount = authorized, cieloCode = "cielo-$orderId", authCode = "auth-$orderId"),
)
