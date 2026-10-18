package dev.railson.ticketsales.payment.cielolio.deeplink

import android.net.Uri
import android.util.Base64
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OrdersDeepLinkTest {

    @Test
    fun `the orders request asks for one page of five and answers on its own host`() {
        val uri = Uri.parse(OrdersDeepLink.orders(page = 3))

        assertThat("${uri.scheme}://${uri.host}").isEqualTo("lio://orders")
        assertThat(uri.getQueryParameter("urlCallback")).isEqualTo("order://orders")
        val request = Json.decodeFromString<OrdersRequest>(
            String(Base64.decode(uri.getQueryParameter("request"), Base64.DEFAULT), Charsets.UTF_8),
        )
        assertThat(request.page).isEqualTo(3)
        assertThat(request.pageSize).isEqualTo(5)
    }
}
