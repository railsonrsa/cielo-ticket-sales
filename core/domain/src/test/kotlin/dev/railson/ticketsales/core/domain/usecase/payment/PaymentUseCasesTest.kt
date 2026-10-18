package dev.railson.ticketsales.core.domain.usecase.payment

import com.google.common.truth.Truth.assertThat
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.model.Money
import dev.railson.ticketsales.core.domain.model.PurchaseItem
import dev.railson.ticketsales.core.domain.model.PurchaseStatus
import dev.railson.ticketsales.core.domain.model.aCieloOrder
import dev.railson.ticketsales.core.domain.model.anEvent
import dev.railson.ticketsales.core.domain.repository.FakeCartRepository
import dev.railson.ticketsales.core.domain.repository.FakeEventRepository
import dev.railson.ticketsales.core.domain.repository.FakePurchaseRepository
import dev.railson.ticketsales.core.domain.terminal.PaymentOutcome
import dev.railson.ticketsales.core.domain.terminal.RecordingPaymentTerminal
import dev.railson.ticketsales.core.domain.usecase.cart.ObserveCartUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PaymentUseCasesTest {

    private val eventRepository = FakeEventRepository()
    private val cartRepository = FakeCartRepository()
    private val purchaseRepository = FakePurchaseRepository(eventRepository, cartRepository)
    private val terminal = RecordingPaymentTerminal(purchaseRepository)
    private var purchaseIds = generateSequence(1) { it + 1 }.map { "purchase-$it" }.iterator()

    private val startPurchase = StartPurchaseUseCase(
        observeCart = ObserveCartUseCase(cartRepository, eventRepository),
        purchaseRepository = purchaseRepository,
        paymentTerminal = terminal,
        newPurchaseId = { purchaseIds.next() },
        now = { NOW },
    )
    private val handleOutcome = HandlePaymentOutcomeUseCase(purchaseRepository)

    @Test
    fun `paying stores the purchase awaiting payment with frozen prices before requesting it`() = runTest {
        eventRepository.setEvents(
            listOf(
                anEvent(id = "show", name = "Show", unitPrice = Money(cents = 4_000)),
                anEvent(id = "play", name = "Peça", unitPrice = Money(cents = 2_550)),
            ),
        )
        cartRepository.setQuantity("show", 2)
        cartRepository.setQuantity("play", 1)

        val result = startPurchase()

        assertThat(result).isEqualTo(StartPurchaseResult.Started(purchaseId = "purchase-1"))
        val stored = purchaseRepository.find("purchase-1")!!
        assertThat(stored.status).isEqualTo(PurchaseStatus.AwaitingPayment)
        assertThat(stored.items).containsExactly(
            PurchaseItem(eventId = "show", eventName = "Show", quantity = 2, unitPrice = Money(cents = 4_000)),
            PurchaseItem(eventId = "play", eventName = "Peça", quantity = 1, unitPrice = Money(cents = 2_550)),
        ).inOrder()
        assertThat(stored.total).isEqualTo(Money(cents = 10_550))
        assertThat(stored.createdAtEpochMillis).isEqualTo(NOW)
        assertThat(terminal.requested).containsExactly(stored)
        assertThat(terminal.storedAtRequest).containsExactly(stored)
    }

    @Test
    fun `an empty cart creates no purchase and requests nothing`() = runTest {
        val result = startPurchase()

        assertThat(result).isEqualTo(StartPurchaseResult.EmptyCart)
        assertThat(purchaseRepository.findAwaitingPayment()).isNull()
        assertThat(terminal.requested).isEmpty()
    }

    @Test
    fun `items that no longer fit the capacity block paying and come back as conflicts`() = runTest {
        eventRepository.setEvents(
            listOf(anEvent(id = "show", capacity = 1), anEvent(id = "play", capacity = 10), anEvent(id = "talk", capacity = 0)),
        )
        cartRepository.setQuantity("show", 2)
        cartRepository.setQuantity("play", 2)
        cartRepository.setQuantity("talk", 1)

        val result = startPurchase()

        assertThat(result).isEqualTo(StartPurchaseResult.CapacityConflicts(eventIds = listOf("show", "talk")))
        assertThat(purchaseRepository.findAwaitingPayment()).isNull()
        assertThat(terminal.requested).isEmpty()
    }

    @Test
    fun `paying again while a purchase awaits payment is refused`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "show")))
        cartRepository.setQuantity("show", 1)
        startPurchase()

        val retry = startPurchase()

        assertThat(retry).isEqualTo(StartPurchaseResult.PaymentInProgress)
        assertThat(terminal.requested.map { it.id }).containsExactly("purchase-1")
    }

    @Test
    fun `a double tap on pay creates a single purchase`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "show")))
        cartRepository.setQuantity("show", 1)

        val results = listOf(async { startPurchase() }, async { startPurchase() }).awaitAll()

        assertThat(results).containsExactly(StartPurchaseResult.Started("purchase-1"), StartPurchaseResult.PaymentInProgress)
        assertThat(terminal.requested.map { it.id }).containsExactly("purchase-1")
    }

    @Test
    fun `an authorized payment for the whole total approves the purchase`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "show", unitPrice = Money(cents = 4_000), capacity = 10)))
        cartRepository.setQuantity("show", 3)
        startPurchase()
        val cieloOrder = aCieloOrder(authorizedAmount = Money(cents = 12_000))

        handleOutcome(PaymentOutcome.Paid(reference = "purchase-1", cieloOrder = cieloOrder))

        val approved = purchaseRepository.find("purchase-1")!!
        assertThat(approved.status).isEqualTo(PurchaseStatus.Approved)
        assertThat(approved.cieloOrder).isEqualTo(cieloOrder)
        assertThat(approved.pendingRefund).isFalse()
        assertThat(cart().items).isEmpty()
        assertThat(eventRepository.observeEvents().first().single().capacity).isEqualTo(7)
    }

    @Test
    fun `an authorized amount different from the total fails the purchase and flags it for refund`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "show", unitPrice = Money(cents = 4_000), capacity = 10)))
        cartRepository.setQuantity("show", 3)
        startPurchase()
        val cieloOrder = aCieloOrder(authorizedAmount = Money(cents = 4_000))

        handleOutcome(PaymentOutcome.Paid(reference = "purchase-1", cieloOrder = cieloOrder))

        val failed = purchaseRepository.find("purchase-1")!!
        assertThat(failed.status).isEqualTo(PurchaseStatus.Failed(FailureReason.AmountMismatch))
        assertThat(failed.cieloOrder).isEqualTo(cieloOrder)
        assertThat(failed.pendingRefund).isTrue()
        assertThat(cart().items.single().quantity).isEqualTo(3)
        assertThat(eventRepository.observeEvents().first().single().capacity).isEqualTo(10)
    }

    @Test
    fun `each terminal failure fails the purchase awaiting payment and keeps the cart`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "show", capacity = 10)))
        cartRepository.setQuantity("show", 2)
        val terminalFailures = listOf(
            FailureReason.CanceledByOperator,
            FailureReason.PaymentError,
            FailureReason.Declined,
            FailureReason.TerminalAuthError,
        )

        terminalFailures.forEachIndexed { index, reason ->
            val purchaseId = "purchase-${index + 1}"
            assertThat(startPurchase()).isEqualTo(StartPurchaseResult.Started(purchaseId))

            handleOutcome(PaymentOutcome.Failed(reason))

            val failed = purchaseRepository.find(purchaseId)!!
            assertThat(failed.status).isEqualTo(PurchaseStatus.Failed(reason))
            assertThat(failed.pendingRefund).isFalse()
            assertThat(cart().items.single().quantity).isEqualTo(2)
        }
    }

    @Test
    fun `an approved purchase ignores repeated callbacks`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "show", unitPrice = Money(cents = 4_000), capacity = 10)))
        cartRepository.setQuantity("show", 2)
        startPurchase()
        val cieloOrder = aCieloOrder(authorizedAmount = Money(cents = 8_000))
        handleOutcome(PaymentOutcome.Paid(reference = "purchase-1", cieloOrder = cieloOrder))
        cartRepository.setQuantity("show", 1)

        handleOutcome(PaymentOutcome.Paid(reference = "purchase-1", cieloOrder = cieloOrder.copy(orderId = "cielo-order-2")))
        handleOutcome(PaymentOutcome.Paid(reference = "purchase-1", cieloOrder = aCieloOrder(authorizedAmount = Money(cents = 1))))
        handleOutcome(PaymentOutcome.Failed(FailureReason.CanceledByOperator))

        val approved = purchaseRepository.find("purchase-1")!!
        assertThat(approved.status).isEqualTo(PurchaseStatus.Approved)
        assertThat(approved.cieloOrder).isEqualTo(cieloOrder)
        assertThat(approved.pendingRefund).isFalse()
        assertThat(eventRepository.observeEvents().first().single().capacity).isEqualTo(8)
        assertThat(cart().items.single().quantity).isEqualTo(1)
    }

    @Test
    fun `an unreadable callback leaves the purchase awaiting payment and paying blocked`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "show")))
        cartRepository.setQuantity("show", 1)
        startPurchase()

        handleOutcome(PaymentOutcome.Unreadable)

        assertThat(purchaseRepository.find("purchase-1")!!.status).isEqualTo(PurchaseStatus.AwaitingPayment)
        assertThat(startPurchase()).isEqualTo(StartPurchaseResult.PaymentInProgress)
    }

    private suspend fun cart() = ObserveCartUseCase(cartRepository, eventRepository)().first()

    private companion object {
        const val NOW = 1_792_000_000_000
    }
}
