package dev.railson.ticketsales.payment.cielolio.deeplink

import android.net.Uri
import android.util.Base64
import com.google.common.truth.Truth.assertThat
import dev.railson.ticketsales.core.domain.model.Money
import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.model.PurchaseItem
import dev.railson.ticketsales.core.domain.model.PurchaseStatus
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentDeepLinkTest {

    @Test
    fun `the payment request carries the purchase id as reference and the exact sum of its items as value`() {
        val purchase = Purchase(
            id = "3f2a9c1e-0000-4000-8000-000000000000",
            status = PurchaseStatus.AwaitingPayment,
            items = listOf(
                PurchaseItem(eventId = "show", eventName = "Show", quantity = 2, unitPrice = Money(4_000)),
                PurchaseItem(eventId = "play", eventName = "Peça", quantity = 3, unitPrice = Money(2_550)),
            ),
            total = Money(15_650),
            createdAtEpochMillis = 0,
        )

        val uri = Uri.parse(PaymentDeepLink.payment(purchase))

        assertThat("${uri.scheme}://${uri.host}").isEqualTo("lio://payment")
        assertThat(uri.getQueryParameter("urlCallback")).isEqualTo("order://response")
        val request = Json.decodeFromString<PaymentRequest>(
            String(Base64.decode(uri.getQueryParameter("request"), Base64.DEFAULT), Charsets.UTF_8),
        )
        assertThat(request.reference).isEqualTo(purchase.id)
        assertThat(request.value).isEqualTo(15_650)
        assertThat(request.items).containsExactly(
            PaymentRequest.Item(sku = "show", name = "Show", quantity = 2, unitPrice = 4_000, unitOfMeasure = "unidade"),
            PaymentRequest.Item(sku = "play", name = "Peça", quantity = 3, unitPrice = 2_550, unitOfMeasure = "unidade"),
        ).inOrder()
    }
}
