package dev.railson.ticketsales.core.domain.usecase.payment

import com.google.common.truth.Truth.assertThat
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.model.Money
import dev.railson.ticketsales.core.domain.model.Purchase
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
import dev.railson.ticketsales.core.domain.usecase.purchase.ObserveLateApprovalsUseCase
import dev.railson.ticketsales.core.domain.usecase.purchase.ObserveSoldOutNoticesUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * O callback autorizado que chega depois de a Purchase virar `Failed` (ADR 0001, item 6): o dinheiro saiu.
 * Quem confirma primeiro leva: com lugar para todos os itens, a Purchase é promovida; senão, tudo vai para estorno.
 * Os cenários são os do simulador `docs/simuladores/aprovacao-atrasada.html`.
 */
class LateApprovalTest {

    private val eventRepository = FakeEventRepository()
    private val cartRepository = FakeCartRepository()
    private val purchaseRepository = FakePurchaseRepository(eventRepository, cartRepository)
    private val terminal = RecordingPaymentTerminal(purchaseRepository)
    private var purchaseIds = generateSequence(1) { it + 1 }.map { "purchase-$it" }.iterator()

    private val observeCart = ObserveCartUseCase(cartRepository, eventRepository)
    private val startPurchase = StartPurchaseUseCase(
        observeCart = observeCart,
        purchaseRepository = purchaseRepository,
        paymentTerminal = terminal,
        newPurchaseId = { purchaseIds.next() },
        now = { 1_792_000_000_000 },
    )
    private val handleOutcome = HandlePaymentOutcomeUseCase(purchaseRepository)
    private val observeLateApprovals = ObserveLateApprovalsUseCase(purchaseRepository)
    private val observeSoldOutNotices = ObserveSoldOutNoticesUseCase(purchaseRepository)
    private val acknowledgeSoldOut = AcknowledgeSoldOutUseCase(purchaseRepository)

    @Test
    fun `a late approval with room for every item approves the purchase without touching the cart`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "jazz", unitPrice = Money(cents = 3_000), capacity = 5)))
        val purchaseA = failedPurchase("jazz" to 2)
        cartRepository.setQuantity("jazz", 1)
        val cieloOrder = aCieloOrder(authorizedAmount = Money(cents = 6_000))

        handleOutcome(PaymentOutcome.Paid(reference = purchaseA, cieloOrder = cieloOrder))

        val approved = purchaseRepository.find(purchaseA)!!
        assertThat(approved.status).isEqualTo(PurchaseStatus.Approved)
        assertThat(approved.cieloOrder).isEqualTo(cieloOrder)
        assertThat(approved.pendingRefund).isFalse()
        assertThat(approved.needsAcknowledgment).isTrue()
        assertThat(purchaseRepository.observeTickets(purchaseA).first()).hasSize(2)
        assertThat(capacityOf("jazz")).isEqualTo(3)
        assertThat(cartRepository.observeQuantities().first()).containsExactly("jazz", 1)
        assertThat(observeLateApprovals().first().map { it.id }).containsExactly(purchaseA)
    }

    @Test
    fun `a late approval without room keeps the purchase failed and flags the whole charge for refund`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "jazz", unitPrice = Money(cents = 3_000), capacity = 5)))
        val purchaseA = failedPurchase("jazz" to 2)
        sellOut("jazz", left = 1)
        val cieloOrder = aCieloOrder(authorizedAmount = Money(cents = 6_000))

        handleOutcome(PaymentOutcome.Paid(reference = purchaseA, cieloOrder = cieloOrder))

        val refund = purchaseRepository.find(purchaseA)!!
        assertThat(refund.status).isEqualTo(PurchaseStatus.Failed(FailureReason.SoldOutAfterPayment))
        assertThat(refund.cieloOrder).isEqualTo(cieloOrder)
        assertThat(refund.pendingRefund).isTrue()
        assertThat(refund.needsAcknowledgment).isTrue()
        assertThat(purchaseRepository.observeTickets(purchaseA).first()).isEmpty()
        assertThat(capacityOf("jazz")).isEqualTo(1)
        assertThat(observeSoldOutNotices().first().map { it.id }).containsExactly(purchaseA)
        assertThat(observeLateApprovals().first()).isEmpty()
    }

    @Test
    fun `missing room in a single event sends the whole purchase to refund, with no partial approval`() = runTest {
        eventRepository.setEvents(
            listOf(
                anEvent(id = "jazz", unitPrice = Money(cents = 3_000), capacity = 5),
                anEvent(id = "talk", unitPrice = Money(cents = 1_000), capacity = 10),
            ),
        )
        val purchaseA = failedPurchase("talk" to 3, "jazz" to 2)
        sellOut("jazz", left = 0)

        handleOutcome(PaymentOutcome.Paid(reference = purchaseA, cieloOrder = aCieloOrder(authorizedAmount = Money(cents = 9_000))))

        assertThat(purchaseRepository.find(purchaseA)!!.status).isEqualTo(PurchaseStatus.Failed(FailureReason.SoldOutAfterPayment))
        assertThat(purchaseRepository.observeTickets(purchaseA).first()).isEmpty()
        assertThat(capacityOf("talk")).isEqualTo(10)
        assertThat(capacityOf("jazz")).isEqualTo(0)
    }

    @Test
    fun `an abandoned purchase is approved late when there is still room`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "jazz", unitPrice = Money(cents = 3_000), capacity = 5)))
        val purchaseA = failedPurchase("jazz" to 2, reason = FailureReason.Abandoned)

        handleOutcome(PaymentOutcome.Paid(reference = purchaseA, cieloOrder = aCieloOrder(authorizedAmount = Money(cents = 6_000))))

        assertThat(purchaseRepository.find(purchaseA)!!.status).isEqualTo(PurchaseStatus.Approved)
    }

    @Test
    fun `a late authorized amount different from the total is flagged for refund`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "jazz", unitPrice = Money(cents = 3_000), capacity = 5)))
        val purchaseA = failedPurchase("jazz" to 2)
        val cieloOrder = aCieloOrder(authorizedAmount = Money(cents = 3_000))

        handleOutcome(PaymentOutcome.Paid(reference = purchaseA, cieloOrder = cieloOrder))

        val refund = purchaseRepository.find(purchaseA)!!
        assertThat(refund.status).isEqualTo(PurchaseStatus.Failed(FailureReason.AmountMismatch))
        assertThat(refund.cieloOrder).isEqualTo(cieloOrder)
        assertThat(refund.pendingRefund).isTrue()
        assertThat(capacityOf("jazz")).isEqualTo(5)
    }

    @Test
    fun `another paid order on a purchase already flagged for refund joins the refund, and a repeated one is ignored`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "jazz", unitPrice = Money(cents = 3_000), capacity = 5)))
        val purchaseA = failedPurchase("jazz" to 2)
        sellOut("jazz", left = 0)
        val first = aCieloOrder(authorizedAmount = Money(cents = 6_000), orderId = "order-1")
        handleOutcome(PaymentOutcome.Paid(reference = purchaseA, cieloOrder = first))

        eventRepository.setEvents(listOf(anEvent(id = "jazz", unitPrice = Money(cents = 3_000), capacity = 5)))
        handleOutcome(PaymentOutcome.Paid(reference = purchaseA, cieloOrder = first.copy(orderId = "order-2")))
        handleOutcome(PaymentOutcome.Paid(reference = purchaseA, cieloOrder = first))
        handleOutcome(PaymentOutcome.Paid(reference = purchaseA, cieloOrder = first.copy(orderId = "order-2")))

        val refund = purchaseRepository.find(purchaseA)!!
        assertThat(refund.status).isEqualTo(PurchaseStatus.Failed(FailureReason.SoldOutAfterPayment))
        assertThat(refund.cieloOrder).isEqualTo(first)
        assertThat(refund.extraOrderIds).containsExactly("order-2")
        assertThat(refund.pendingRefund).isTrue()
        assertThat(refund.needsAcknowledgment).isTrue()
        assertThat(capacityOf("jazz")).isEqualTo(5)
    }

    @Test
    fun `a purchase approved late ignores the repeated callback`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "jazz", unitPrice = Money(cents = 3_000), capacity = 5)))
        val purchaseA = failedPurchase("jazz" to 2)
        val cieloOrder = aCieloOrder(authorizedAmount = Money(cents = 6_000))
        handleOutcome(PaymentOutcome.Paid(reference = purchaseA, cieloOrder = cieloOrder))

        handleOutcome(PaymentOutcome.Paid(reference = purchaseA, cieloOrder = cieloOrder))

        assertThat(purchaseRepository.observeTickets(purchaseA).first()).hasSize(2)
        assertThat(capacityOf("jazz")).isEqualTo(3)
    }

    @Test
    fun `an on-time approval that no longer fits goes to refund instead of selling past the capacity`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "jazz", unitPrice = Money(cents = 3_000), capacity = 5)))
        cartRepository.setQuantity("jazz", 2)
        val purchaseB = (startPurchase() as StartPurchaseResult.Started).purchaseId
        // Enquanto B espera, uma aprovação atrasada leva os lugares.
        sellOut("jazz", left = 1)

        handleOutcome(PaymentOutcome.Paid(reference = purchaseB, cieloOrder = aCieloOrder(authorizedAmount = Money(cents = 6_000))))

        val refund = purchaseRepository.find(purchaseB)!!
        assertThat(refund.status).isEqualTo(PurchaseStatus.Failed(FailureReason.SoldOutAfterPayment))
        assertThat(refund.pendingRefund).isTrue()
        assertThat(refund.needsAcknowledgment).isTrue()
        assertThat(capacityOf("jazz")).isEqualTo(1)
        assertThat(cartRepository.observeQuantities().first()).containsExactly("jazz", 2)
    }

    @Test
    fun `paying after a late approval asks to review the cart and creates nothing until the operator accepts`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "jazz", unitPrice = Money(cents = 3_000), capacity = 5)))
        val purchaseA = approvedLate("jazz" to 2)
        cartRepository.setQuantity("jazz", 2)

        val review = startPurchase()
        val again = startPurchase()

        val lateApproval = purchaseRepository.find(purchaseA)!!
        assertThat(review).isEqualTo(StartPurchaseResult.LateApprovalsToReview(listOf(lateApproval)))
        assertThat(again).isEqualTo(review)
        assertThat(terminal.requested).isEmpty()
        assertThat(purchaseRepository.findAwaitingPayment()).isNull()
    }

    @Test
    fun `accepting the late approvals records the acceptance and pays`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "jazz", unitPrice = Money(cents = 3_000), capacity = 5)))
        val purchaseA = approvedLate("jazz" to 2)
        cartRepository.setQuantity("jazz", 1)

        val result = startPurchase(acceptedLateApprovals = setOf(purchaseA))

        assertThat(result).isInstanceOf(StartPurchaseResult.Started::class.java)
        assertThat(purchaseRepository.find(purchaseA)!!.needsAcknowledgment).isFalse()
        assertThat(observeLateApprovals().first()).isEmpty()
        handleOutcome(PaymentOutcome.Failed(FailureReason.CanceledByOperator))
        assertThat(startPurchase()).isInstanceOf(StartPurchaseResult.Started::class.java)
    }

    @Test
    fun `a late approval that arrives after the dialog opened still has to be accepted`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "jazz", unitPrice = Money(cents = 3_000), capacity = 5)))
        val purchaseA = approvedLate("jazz" to 1)
        val purchaseC = approvedLate("jazz" to 1)
        cartRepository.setQuantity("jazz", 1)

        val result = startPurchase(acceptedLateApprovals = setOf(purchaseA))

        assertThat(result).isEqualTo(StartPurchaseResult.LateApprovalsToReview(listOf(purchaseRepository.find(purchaseC)!!)))
        assertThat(purchaseRepository.find(purchaseA)!!.needsAcknowledgment).isFalse()
        assertThat(terminal.requested).isEmpty()
    }

    @Test
    fun `understood clears the sold out notice and leaves the refund flag`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "jazz", unitPrice = Money(cents = 3_000), capacity = 5)))
        val purchaseA = failedPurchase("jazz" to 2)
        sellOut("jazz", left = 0)
        handleOutcome(PaymentOutcome.Paid(reference = purchaseA, cieloOrder = aCieloOrder(authorizedAmount = Money(cents = 6_000))))

        acknowledgeSoldOut(purchaseA)

        val refund = purchaseRepository.find(purchaseA)!!
        assertThat(refund.needsAcknowledgment).isFalse()
        assertThat(refund.pendingRefund).isTrue()
        assertThat(observeSoldOutNotices().first()).isEmpty()
    }

    @Test
    fun `understood does not accept a late approval for the operator`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "jazz", unitPrice = Money(cents = 3_000), capacity = 5)))
        val purchaseA = approvedLate("jazz" to 1)

        acknowledgeSoldOut(purchaseA)

        assertThat(purchaseRepository.find(purchaseA)!!.needsAcknowledgment).isTrue()
    }

    /** Uma Purchase que o app deu como `Failed`, mas que a Cielo cobrou. Os itens já saíram do Cart. */
    private suspend fun failedPurchase(vararg items: Pair<String, Int>, reason: FailureReason = FailureReason.NotCompleted): String {
        val events = eventRepository.observeEvents().first()
        val purchaseItems = items.map { (eventId, quantity) ->
            val event = events.single { it.id == eventId }
            PurchaseItem(event.id, event.name, quantity, event.unitPrice)
        }
        val purchase = Purchase(
            id = purchaseIds.next(),
            status = PurchaseStatus.Failed(reason),
            items = purchaseItems,
            total = Money(purchaseItems.sumOf { it.unitPrice.cents * it.quantity }),
            createdAtEpochMillis = 1_791_000_000_000 + purchaseItems.size,
        )
        purchaseRepository.create(purchase)
        return purchase.id
    }

    private suspend fun approvedLate(vararg items: Pair<String, Int>): String {
        val purchaseId = failedPurchase(*items)
        val total = purchaseRepository.find(purchaseId)!!.total
        handleOutcome(PaymentOutcome.Paid(reference = purchaseId, cieloOrder = aCieloOrder(authorizedAmount = total, orderId = "order-$purchaseId")))
        return purchaseId
    }

    /** Outra venda, aprovada enquanto A estava `Failed`, deixa só [left] lugares. */
    private suspend fun sellOut(eventId: String, left: Int) {
        eventRepository.setEvents(eventRepository.observeEvents().first().map { if (it.id == eventId) it.copy(capacity = left) else it })
    }

    private suspend fun capacityOf(eventId: String) = eventRepository.observeEvents().first().single { it.id == eventId }.capacity
}
