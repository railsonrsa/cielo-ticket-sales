package dev.railson.ticketsales.core.domain.terminal

import dev.railson.ticketsales.core.domain.model.CieloOrder

/** Uma página do `lio://orders`, já traduzida para o domínio. A ordem das Orders não é documentada pela Cielo. */
data class OrdersPage(
    val number: Int,
    val totalPages: Int,
    val orders: List<ListedOrder>,
)

/**
 * Uma Cielo Order da lista. Sem pagamento autorizado ([CieloOrder.authorizedAmount] zero) ela só entrou (`ENTERED`):
 * a Cielo cria a Order mesmo quando o pagamento é cancelado ou abandonado.
 */
data class ListedOrder(
    val reference: String?,
    val createdAtEpochMillis: Long?,
    val cieloOrder: CieloOrder,
) {

    val isPaid: Boolean get() = cieloOrder.authorizedAmount.cents > 0
}
