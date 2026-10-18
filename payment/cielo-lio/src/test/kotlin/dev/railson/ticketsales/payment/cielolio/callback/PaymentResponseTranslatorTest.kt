package dev.railson.ticketsales.payment.cielolio.callback

import com.google.common.truth.Truth.assertThat
import dev.railson.ticketsales.core.domain.model.CieloOrder
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.model.Money
import dev.railson.ticketsales.core.domain.terminal.PaymentOutcome
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** As respostas que o emulador Cielo 1.61.8 devolveu de verdade (ver `fixtures/README.md`). */
@RunWith(RobolectricTestRunner::class)
class PaymentResponseTranslatorTest {

    @Test
    fun `an approved payment is paid for its reference with the authorized amount, despite paidAmount 0`() {
        val outcome = PaymentResponseTranslator.translate(fixtureBase64("payment-approved.json"))

        assertThat(outcome).isEqualTo(
            PaymentOutcome.Paid(
                reference = "6e46c58a-ca07-43be-9242-d12aee9d96fd",
                cieloOrder = CieloOrder(
                    orderId = "cbd76f56-d782-4cf9-8db8-f76615be44d7",
                    authorizedAmount = Money(5_000),
                    cieloCode = "jRZjCa",
                    authCode = "gUA2RP",
                ),
            ),
        )
    }

    @Test
    fun `a payment for two events authorizes the whole value`() {
        val outcome = PaymentResponseTranslator.translate(fixtureBase64("payment-approved-two-events.json")) as PaymentOutcome.Paid

        assertThat(outcome.reference).isEqualTo("e1a2957f-3eba-4364-b59f-49d0aff2d81d")
        assertThat(outcome.cieloOrder.authorizedAmount).isEqualTo(Money(19_000))
    }

    @Test
    fun `a value below the items comes back PAID but authorizes only what was charged`() {
        // C6b: a Cielo cobrou R$ 100 de R$ 190 em itens e respondeu PAID com pendingAmount 0.
        val outcome = PaymentResponseTranslator.translate(fixtureBase64("payment-value-below-items.json")) as PaymentOutcome.Paid

        assertThat(outcome.cieloOrder.authorizedAmount).isEqualTo(Money(10_000))
    }

    @Test
    fun `a duplicate charge comes back as two paid outcomes for the same reference with different orders`() {
        val first = PaymentResponseTranslator.translate(fixtureBase64("payment-duplicate-first.json")) as PaymentOutcome.Paid
        val second = PaymentResponseTranslator.translate(fixtureBase64("payment-duplicate-second.json")) as PaymentOutcome.Paid

        assertThat(second.reference).isEqualTo(first.reference)
        assertThat(second.cieloOrder.orderId).isNotEqualTo(first.cieloOrder.orderId)
        assertThat(second.cieloOrder.authCode).isNotEqualTo(first.cieloOrder.authCode)
    }

    @Test
    fun `only payments with an authorized status code count`() {
        val canceledPayment = """
            {"id":"order-1","reference":"purchase-1","status":"PAID","paidAmount":5000,"payments":[
              {"amount":5000,"authCode":"a1","cieloCode":"c1","paymentFields":{"statusCode":"2"}},
              {"amount":3000,"authCode":"a2","cieloCode":"c2","paymentFields":{"statusCode":"0"}}
            ]}
        """.trimIndent()

        val outcome = PaymentResponseTranslator.translate(base64(canceledPayment)) as PaymentOutcome.Paid

        assertThat(outcome.cieloOrder).isEqualTo(CieloOrder("order-1", Money(3_000), cieloCode = "c2", authCode = "a2"))
    }

    @Test
    fun `each failure code becomes its reason`() {
        val expected = mapOf(
            "failure-canceled.json" to FailureReason.CanceledByOperator,
            "failure-error.json" to FailureReason.PaymentError,
            "failure-declined.json" to FailureReason.Declined,
            "failure-auth.json" to FailureReason.TerminalAuthError,
        )

        expected.forEach { (fixture, reason) ->
            assertThat(PaymentResponseTranslator.translate(fixtureBase64(fixture))).isEqualTo(PaymentOutcome.Failed(reason))
        }
    }

    @Test
    fun `an unknown failure code is not a certain failure`() {
        assertThat(PaymentResponseTranslator.translate(base64("""{"code":9,"reason":"?"}"""))).isEqualTo(PaymentOutcome.Unreadable)
    }

    @Test
    fun `a missing, broken or unknown response is unreadable`() {
        val unreadable = listOf(
            null,
            "",
            "%%%não é base64%%%",
            base64("não é json"),
            base64("""["um","array"]"""),
            base64("""{"responsecode":0}"""),
            base64("""{"id":"order-1","payments":[]}"""),
            base64("""{"id":"order-1","reference":"purchase-1","payments":"x"}"""),
        )

        unreadable.forEach { response ->
            assertThat(PaymentResponseTranslator.translate(response)).isEqualTo(PaymentOutcome.Unreadable)
        }
    }

    @Test
    fun `plus signs turned into spaces by the query decoding still decode`() {
        val base64 = base64("""{"code":1,"reason":">>>???"}""")
        assertThat(base64).contains("+")

        assertThat(PaymentResponseTranslator.translate(base64.replace('+', ' ')))
            .isEqualTo(PaymentOutcome.Failed(FailureReason.CanceledByOperator))
    }
}
