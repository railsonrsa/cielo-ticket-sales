package dev.railson.ticketsales.core.domain.usecase.payment

import com.google.common.truth.Truth.assertThat
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.model.Money
import dev.railson.ticketsales.core.domain.model.PurchaseStatus
import dev.railson.ticketsales.core.domain.model.aCieloOrder
import dev.railson.ticketsales.core.domain.model.aPurchase
import dev.railson.ticketsales.core.domain.model.anEvent
import dev.railson.ticketsales.core.domain.repository.FakeCartRepository
import dev.railson.ticketsales.core.domain.repository.FakeEventRepository
import dev.railson.ticketsales.core.domain.repository.FakePurchaseRepository
import dev.railson.ticketsales.core.domain.terminal.CieloOrdersStub
import dev.railson.ticketsales.core.domain.terminal.PaymentOutcome
import dev.railson.ticketsales.core.domain.terminal.RecordingPaymentTerminal
import dev.railson.ticketsales.core.domain.terminal.anOrderFor
import dev.railson.ticketsales.core.domain.terminal.anotherOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ReconcilePurchaseUseCaseTest {

    private val eventRepository = FakeEventRepository()
    private val cartRepository = FakeCartRepository()
    private val purchaseRepository = FakePurchaseRepository(eventRepository, cartRepository)
    private val terminal = RecordingPaymentTerminal(purchaseRepository)
    private val handleOutcome = HandlePaymentOutcomeUseCase(purchaseRepository)

    private fun TestScope.newReconcile() = ReconcilePurchaseUseCase(
        purchaseRepository = purchaseRepository,
        paymentTerminal = terminal,
        scope = backgroundScope,
    )

    /** Uma Purchase `AwaitingPayment` de R$ 50, criada em [PURCHASED_AT], com o item ainda no Cart. */
    private suspend fun givenOrphan() {
        eventRepository.setEvents(listOf(anEvent(id = "event-1", capacity = 10)))
        cartRepository.setQuantity("event-1", 1)
        purchaseRepository.create(aPurchase(id = ORPHAN, status = PurchaseStatus.AwaitingPayment, createdAtEpochMillis = PURCHASED_AT))
    }

    /** Faz o papel da Cielo: responde cada página pedida, até o use case parar de pedir. */
    private suspend fun ReconcilePurchaseUseCase.answeredBy(cielo: CieloOrdersStub) {
        var answered = 0
        while (answered < terminal.requestedPages.size) {
            onOrdersPage(cielo.page(terminal.requestedPages[answered++]))
        }
    }

    private suspend fun orphan() = purchaseRepository.find(ORPHAN)!!

    // Desfechos

    @Test
    fun `an order paying the whole total approves the orphan`() = runTest {
        givenOrphan()
        val reconcile = newReconcile()

        assertThat(reconcile()).isEqualTo(StartReconciliationResult.Started)
        reconcile.answeredBy(CieloOrdersStub(listOf(anotherOrder(1), anOrderFor(ORPHAN, TOTAL, orderId = "paid"))))

        val purchase = orphan()
        assertThat(purchase.status).isEqualTo(PurchaseStatus.Approved)
        assertThat(purchase.cieloOrder?.orderId).isEqualTo("paid")
        assertThat(purchase.pendingRefund).isFalse()
        assertThat(purchaseRepository.observeTickets(ORPHAN).first()).hasSize(1)
        assertThat(cartRepository.observeQuantities().first()).isEmpty()
        assertThat(reconcile.isRunning.value).isFalse()
    }

    @Test
    fun `an order that only entered fails the orphan as not completed and keeps the cart`() = runTest {
        givenOrphan()
        val reconcile = newReconcile()

        reconcile()
        reconcile.answeredBy(CieloOrdersStub(listOf(anOrderFor(ORPHAN, authorized = Money(0)))))

        assertThat(orphan().status).isEqualTo(PurchaseStatus.Failed(FailureReason.NotCompleted))
        assertThat(orphan().pendingRefund).isFalse()
        assertThat(cartRepository.observeQuantities().first()).containsExactly("event-1", 1)
    }

    @Test
    fun `no order with the reference fails the orphan as not started`() = runTest {
        givenOrphan()
        val reconcile = newReconcile()

        reconcile()
        reconcile.answeredBy(CieloOrdersStub(List(12) { anotherOrder(it) }))

        assertThat(orphan().status).isEqualTo(PurchaseStatus.Failed(FailureReason.NotStarted))
    }

    @Test
    fun `an empty order list fails the orphan as not started`() = runTest {
        givenOrphan()
        val reconcile = newReconcile()

        reconcile()
        reconcile.answeredBy(CieloOrdersStub(emptyList()))

        assertThat(terminal.requestedPages).containsExactly(1)
        assertThat(orphan().status).isEqualTo(PurchaseStatus.Failed(FailureReason.NotStarted))
    }

    @Test
    fun `two paid orders approve with the first one and flag the other for refund`() = runTest {
        givenOrphan()
        val reconcile = newReconcile()
        // A cobrança duplicada cai na página vizinha: 5 Orders por página, a primeira paga é a 5ª.
        val orders = List(4) { anotherOrder(it) } +
            anOrderFor(ORPHAN, TOTAL, orderId = "first") +
            anOrderFor(ORPHAN, TOTAL, orderId = "second") +
            List(4) { anotherOrder(10 + it) }

        reconcile()
        reconcile.answeredBy(CieloOrdersStub(orders))

        val purchase = orphan()
        assertThat(purchase.status).isEqualTo(PurchaseStatus.Approved)
        assertThat(purchase.cieloOrder?.orderId).isEqualTo("first")
        assertThat(purchase.pendingRefund).isTrue()
        assertThat(purchase.extraOrderIds).containsExactly("second")
        assertThat(purchaseRepository.observeTickets(ORPHAN).first()).hasSize(1)
    }

    @Test
    fun `an authorized amount different from the total fails the orphan and flags it for refund`() = runTest {
        givenOrphan()
        val reconcile = newReconcile()

        reconcile()
        reconcile.answeredBy(CieloOrdersStub(listOf(anOrderFor(ORPHAN, authorized = Money(2_500), orderId = "half"))))

        val purchase = orphan()
        assertThat(purchase.status).isEqualTo(PurchaseStatus.Failed(FailureReason.AmountMismatch))
        assertThat(purchase.cieloOrder?.orderId).isEqualTo("half")
        assertThat(purchase.pendingRefund).isTrue()
        assertThat(purchaseRepository.observeTickets(ORPHAN).first()).isEmpty()
    }

    @Test
    fun `a failed query leaves the orphan awaiting payment and counts the check`() = runTest {
        givenOrphan()
        val reconcile = newReconcile()

        reconcile()
        reconcile.onOrdersFailed()

        assertThat(orphan().status).isEqualTo(PurchaseStatus.AwaitingPayment)
        assertThat(orphan().inconclusiveChecks).isEqualTo(1)
        assertThat(reconcile.isRunning.value).isFalse()
    }

    @Test
    fun `a query the terminal never answers ends inconclusive after the timeout`() = runTest {
        givenOrphan()
        val reconcile = newReconcile()

        reconcile()
        advanceTimeBy(ReconcilePurchaseUseCase.PAGE_TIMEOUT_MILLIS - 1)
        runCurrent()
        assertThat(reconcile.isRunning.value).isTrue()

        advanceTimeBy(2)
        runCurrent()

        assertThat(reconcile.isRunning.value).isFalse()
        assertThat(orphan().status).isEqualTo(PurchaseStatus.AwaitingPayment)
        assertThat(orphan().inconclusiveChecks).isEqualTo(1)
    }

    @Test
    fun `a search that reaches 20 pages without an answer ends inconclusive`() = runTest {
        givenOrphan()
        val reconcile = newReconcile()

        reconcile()
        // Sem createdAt não há atalho de parada: só o limite de páginas.
        reconcile.answeredBy(CieloOrdersStub(List(5 * 30) { anotherOrder(it) }))

        assertThat(terminal.requestedPages).hasSize(20)
        assertThat(orphan().status).isEqualTo(PurchaseStatus.AwaitingPayment)
        assertThat(orphan().inconclusiveChecks).isEqualTo(1)
    }

    // Busca pelas duas pontas

    @Test
    fun `the search reads from both ends towards the middle until the ends meet`() = runTest {
        givenOrphan()
        val reconcile = newReconcile()

        reconcile()
        reconcile.answeredBy(CieloOrdersStub(List(5 * 6) { anotherOrder(it) }))

        assertThat(terminal.requestedPages).containsExactly(1, 6, 2, 5, 3, 4).inOrder()
        assertThat(orphan().status).isEqualTo(PurchaseStatus.Failed(FailureReason.NotStarted))
    }

    @Test
    fun `the newest order is found on the last page when the list is oldest first`() = runTest {
        givenOrphan()
        val reconcile = newReconcile()
        val orders = List(5 * 9 + 4) { anotherOrder(it) } + anOrderFor(ORPHAN, TOTAL)

        reconcile()
        reconcile.answeredBy(CieloOrdersStub(orders))

        // Achou na 10, lê a vizinha 9 para pegar uma cobrança duplicada, e para.
        assertThat(terminal.requestedPages).containsExactly(1, 10, 9).inOrder()
        assertThat(orphan().status).isEqualTo(PurchaseStatus.Approved)
    }

    @Test
    fun `the newest order is found on the first page when the list is newest first`() = runTest {
        givenOrphan()
        val reconcile = newReconcile()
        val orders = listOf(anOrderFor(ORPHAN, TOTAL)) + List(5 * 9 + 4) { anotherOrder(it) }

        reconcile()
        reconcile.answeredBy(CieloOrdersStub(orders))

        // Achou na 1: só falta a vizinha 2.
        assertThat(terminal.requestedPages).containsExactly(1, 2).inOrder()
        assertThat(orphan().status).isEqualTo(PurchaseStatus.Approved)
    }

    @Test
    fun `the search stops once both ends are past the purchase time, oldest first`() = runTest {
        givenOrphan()
        val reconcile = newReconcile()
        // 10 páginas, todas de antes da Purchase: depois da 1ª e da última, o meio não tem como conter a Order.
        val orders = List(50) { anotherOrder(it, createdAtEpochMillis = PURCHASED_AT - HOUR * (50 - it)) }

        reconcile()
        reconcile.answeredBy(CieloOrdersStub(orders))

        assertThat(terminal.requestedPages).containsExactly(1, 10).inOrder()
        assertThat(orphan().status).isEqualTo(PurchaseStatus.Failed(FailureReason.NotStarted))
    }

    @Test
    fun `the search stops once both ends are past the purchase time, newest first`() = runTest {
        givenOrphan()
        val reconcile = newReconcile()
        val orders = List(50) { anotherOrder(it, createdAtEpochMillis = PURCHASED_AT - HOUR * (it + 1)) }

        reconcile()
        reconcile.answeredBy(CieloOrdersStub(orders))

        assertThat(terminal.requestedPages).containsExactly(1, 10).inOrder()
        assertThat(orphan().status).isEqualTo(PurchaseStatus.Failed(FailureReason.NotStarted))
    }

    @Test
    fun `the search keeps reading while the unread middle may hold the purchase time`() = runTest {
        givenOrphan()
        val reconcile = newReconcile()
        // Metade antes e metade depois da Purchase, e nenhuma Order dela: o meio precisa ser lido.
        val orders = List(50) { anotherOrder(it, createdAtEpochMillis = PURCHASED_AT + HOUR * (it - 25)) }

        reconcile()
        reconcile.answeredBy(CieloOrdersStub(orders))

        assertThat(terminal.requestedPages).containsExactly(1, 10, 2, 9, 3, 8, 4, 7, 5, 6).inOrder()
        assertThat(orphan().status).isEqualTo(PurchaseStatus.Failed(FailureReason.NotStarted))
    }

    @Test
    fun `an order a few minutes off the purchase time is still searched for`() = runTest {
        givenOrphan()
        val reconcile = newReconcile()
        // A Order nasceu 3 minutos depois da Purchase, no meio da lista: dentro da folga, a busca não para antes.
        val before = List(22) { anotherOrder(it, createdAtEpochMillis = PURCHASED_AT - HOUR * (22 - it)) }
        val after = List(27) { anotherOrder(100 + it, createdAtEpochMillis = PURCHASED_AT + HOUR * (it + 1)) }
        val orders = before + anOrderFor(ORPHAN, TOTAL, createdAtEpochMillis = PURCHASED_AT + 3 * MINUTE) + after

        reconcile()
        reconcile.answeredBy(CieloOrdersStub(orders))

        assertThat(orphan().status).isEqualTo(PurchaseStatus.Approved)
    }

    // Gatilhos e concorrência

    @Test
    fun `without an orphan there is nothing to reconcile`() = runTest {
        val reconcile = newReconcile()

        assertThat(reconcile()).isEqualTo(StartReconciliationResult.NothingToReconcile)
        assertThat(terminal.requestedPages).isEmpty()
    }

    @Test
    fun `checking again while a search runs does not start another one`() = runTest {
        givenOrphan()
        val reconcile = newReconcile()

        reconcile()

        assertThat(reconcile()).isEqualTo(StartReconciliationResult.AlreadyRunning)
        assertThat(terminal.requestedPages).containsExactly(1)
    }

    @Test
    fun `the cold start reconciles only once per process`() = runTest {
        givenOrphan()
        val reconcile = newReconcile()

        reconcile.onColdStart()
        reconcile.onOrdersFailed()
        reconcile.onColdStart()

        assertThat(terminal.requestedPages).containsExactly(1)
    }

    @Test
    fun `a page that arrives with no search running is ignored`() = runTest {
        givenOrphan()
        val reconcile = newReconcile()

        reconcile.onOrdersPage(CieloOrdersStub(listOf(anOrderFor(ORPHAN, authorized = Money(0)))).page(1))
        reconcile.onOrdersFailed()

        assertThat(orphan().status).isEqualTo(PurchaseStatus.AwaitingPayment)
        assertThat(orphan().inconclusiveChecks).isEqualTo(0)
    }

    @Test
    fun `a payment callback that arrives during the search wins over it`() = runTest {
        givenOrphan()
        val reconcile = newReconcile()

        reconcile()
        handleOutcome(PaymentOutcome.Paid(ORPHAN, aCieloOrder(TOTAL, orderId = "callback")))
        reconcile.answeredBy(CieloOrdersStub(listOf(anOrderFor(ORPHAN, authorized = Money(0)))))

        assertThat(orphan().status).isEqualTo(PurchaseStatus.Approved)
        assertThat(orphan().cieloOrder?.orderId).isEqualTo("callback")
    }

    private companion object {
        const val ORPHAN = "orphan"
        val TOTAL = Money(cents = 5_000)
        const val PURCHASED_AT = 1_792_000_000_000
        const val MINUTE = 60_000L
        const val HOUR = 60 * MINUTE
    }
}
