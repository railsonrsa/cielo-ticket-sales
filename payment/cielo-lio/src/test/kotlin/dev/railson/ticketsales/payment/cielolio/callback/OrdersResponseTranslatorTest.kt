package dev.railson.ticketsales.payment.cielolio.callback

import com.google.common.truth.Truth.assertThat
import dev.railson.ticketsales.core.domain.model.CieloOrder
import dev.railson.ticketsales.core.domain.model.Money
import dev.railson.ticketsales.core.domain.terminal.ListedOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.TimeZone

/** As páginas do `lio://orders` que o emulador Cielo 1.61.8 devolveu de verdade (ver `fixtures/README.md`). */
@RunWith(RobolectricTestRunner::class)
class OrdersResponseTranslatorTest {

    private val saoPaulo = TimeZone.getTimeZone("America/Sao_Paulo")

    @Test
    fun `a page carries its number, the total of pages and its orders in the list order`() {
        val page = OrdersResponseTranslator.translate(fixtureBase64("orders-page-1.json"), saoPaulo)!!

        assertThat(page.number).isEqualTo(1)
        assertThat(page.totalPages).isEqualTo(2)
        // Com pageSize 5 vêm 6: a Order-semente `id: "1"` do emulador entra a mais na primeira página.
        assertThat(page.orders.map { it.cieloOrder.orderId }).containsExactly(
            "1",
            "0e373937-5565-41f5-af4f-f5ce77a6aafe",
            "57a36eca-c33c-40e2-b08e-3be5c548cbe0",
            "8819bbfc-3d28-46b0-83ef-570090023945",
            "a7a647c4-f0d5-4a82-b44d-b6ca1a91c55e",
            "76194859-f5a9-4809-965d-8c970da84d36",
        ).inOrder()
    }

    @Test
    fun `a paid order sums its authorized payments, despite paidAmount 0`() {
        val page = OrdersResponseTranslator.translate(fixtureBase64("orders-page-1.json"), saoPaulo)!!

        assertThat(page.orders[1]).isEqualTo(
            ListedOrder(
                reference = "e1b75b25-11fc-4d7c-828f-2c97268a5e03",
                createdAtEpochMillis = saoPauloMillis(16, 44, 44),
                cieloOrder = CieloOrder(
                    orderId = "0e373937-5565-41f5-af4f-f5ce77a6aafe",
                    authorizedAmount = Money(4_000),
                    cieloCode = "6HQBsF",
                    authCode = "5lq0Xf",
                ),
            ),
        )
        assertThat(page.orders[1].isPaid).isTrue()
    }

    @Test
    fun `an order that only entered has nothing authorized`() {
        val page = OrdersResponseTranslator.translate(fixtureBase64("orders-page-1.json"), saoPaulo)!!
        val entered = page.orders.single { it.reference == "10e77f9a-a353-44b0-a70c-e7bcf2ad014c" }

        assertThat(entered.isPaid).isFalse()
        assertThat(entered.cieloOrder.authorizedAmount).isEqualTo(Money(0))
        assertThat(entered.createdAtEpochMillis).isEqualTo(saoPauloMillis(16, 52, 13))
    }

    @Test
    fun `the last page keeps the oldest first order and repeats the last order of the page before`() {
        val first = OrdersResponseTranslator.translate(fixtureBase64("orders-page-1.json"), saoPaulo)!!
        val last = OrdersResponseTranslator.translate(fixtureBase64("orders-page-2.json"), saoPaulo)!!

        assertThat(last.number).isEqualTo(2)
        assertThat(last.orders.first()).isEqualTo(first.orders.last())
        val createdAt = (first.orders + last.orders).mapNotNull { it.createdAtEpochMillis }
        assertThat(createdAt).isInOrder()
    }

    @Test
    fun `a page past the last one comes back empty`() {
        val page = OrdersResponseTranslator.translate(fixtureBase64("orders-page-beyond-last.json"), saoPaulo)!!

        assertThat(page.orders).isEmpty()
        assertThat(page.totalPages).isEqualTo(2)
    }

    @Test
    fun `a created at with a narrow space before the period is still read`() {
        val json = """
            {"currentPage":1,"totalItems":1,"totalPages":1,"results":[
              {"id":"o1","reference":"r1","createdAt":"Oct 18, 2026 4:44:44${' '}PM","payments":[]}
            ]}
        """.trimIndent()

        val page = OrdersResponseTranslator.translate(base64(json), saoPaulo)!!

        assertThat(page.orders.single().createdAtEpochMillis).isEqualTo(saoPauloMillis(16, 44, 44))
    }

    @Test
    fun `an unreadable created at keeps the order without a date`() {
        val json = """{"currentPage":1,"totalItems":1,"totalPages":1,"results":[{"id":"o1","reference":"r1","createdAt":"ontem","payments":[]}]}"""

        val page = OrdersResponseTranslator.translate(base64(json), saoPaulo)!!

        assertThat(page.orders.single().createdAtEpochMillis).isNull()
        assertThat(page.orders.single().reference).isEqualTo("r1")
    }

    @Test
    fun `an error or an unreadable response is not a page`() {
        assertThat(OrdersResponseTranslator.translate(fixtureBase64("failure-error.json"))).isNull()
        assertThat(OrdersResponseTranslator.translate("isto não é base64")).isNull()
        assertThat(OrdersResponseTranslator.translate(null)).isNull()
    }

    private fun saoPauloMillis(hour: Int, minute: Int, second: Int): Long =
        ZonedDateTime.of(2026, 10, 18, hour, minute, second, 0, ZoneId.of("America/Sao_Paulo")).toInstant().toEpochMilli()
}
