package dev.railson.ticketsales.feature.purchases.presentation.purchases

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
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
import dev.railson.ticketsales.core.domain.terminal.PaymentOutcome
import dev.railson.ticketsales.core.domain.terminal.RecordingPaymentTerminal
import dev.railson.ticketsales.core.domain.usecase.payment.AbandonPurchaseUseCase
import dev.railson.ticketsales.core.domain.usecase.payment.AcknowledgeSoldOutUseCase
import dev.railson.ticketsales.core.domain.usecase.payment.HandlePaymentOutcomeUseCase
import dev.railson.ticketsales.core.domain.usecase.payment.ReconcilePurchaseUseCase
import dev.railson.ticketsales.core.domain.usecase.purchase.ObservePurchasesUseCase
import dev.railson.ticketsales.core.domain.usecase.purchase.ObserveSoldOutNoticesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PurchasesViewModelTest {

    private val eventRepository = FakeEventRepository()
    private val purchaseRepository = FakePurchaseRepository(eventRepository, FakeCartRepository())
    private val terminal = RecordingPaymentTerminal(purchaseRepository)
    private lateinit var reconcile: ReconcilePurchaseUseCase

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts loading and then shows the purchases in the order of the list`() = runTest {
        purchaseRepository.create(aPurchase(id = "approved", createdAtEpochMillis = 2_000))
        purchaseRepository.create(aPurchase(id = "declined", status = PurchaseStatus.Failed(FailureReason.Declined)))
        purchaseRepository.create(aPurchase(id = "awaiting", status = PurchaseStatus.AwaitingPayment, createdAtEpochMillis = 1_000))

        purchasesViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(PurchasesUiState(isLoading = true))

            val loaded = awaitItem()
            assertThat(loaded.isLoading).isFalse()
            assertThat(loaded.purchases.map { it.id }).containsExactly("awaiting", "approved").inOrder()
        }
    }

    @Test
    fun `a purchase approved while the list is open moves out of the top`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "event-1")))
        purchaseRepository.create(aPurchase(id = "old", createdAtEpochMillis = 1_000))
        purchaseRepository.create(aPurchase(id = "new", status = PurchaseStatus.AwaitingPayment, createdAtEpochMillis = 2_000))

        purchasesViewModel().uiState.test {
            skipItems(1)
            assertThat(awaitItem().purchases.first().status).isEqualTo(PurchaseStatus.AwaitingPayment)

            purchaseRepository.approve("new", aCieloOrder(authorizedAmount = aPurchase(id = "new").total))

            val shown = awaitItem().purchases
            assertThat(shown.map { it.id }).containsExactly("new", "old").inOrder()
            assertThat(shown.first().status).isEqualTo(PurchaseStatus.Approved)
        }
    }

    @Test
    fun `no purchases stops loading with an empty list`() = runTest {
        purchasesViewModel().uiState.test {
            skipItems(1)
            assertThat(awaitItem()).isEqualTo(PurchasesUiState(isLoading = false, purchases = emptyList()))
        }
    }

    @Test
    fun `checking the orphan at the top asks the terminal for the order list and shows progress`() = runTest {
        purchaseRepository.create(aPurchase(id = "orphan", status = PurchaseStatus.AwaitingPayment))
        val viewModel = purchasesViewModel()

        viewModel.uiState.test {
            viewModel.onAction(PurchasesAction.CheckPayment)
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertThat(terminal.requestedPages).containsExactly(1)
            assertThat(state.isCheckingPayment).isTrue()
            assertThat(state.canAbandon).isFalse()
        }
    }

    @Test
    fun `after an inconclusive check the operator can give up on the orphan, with confirmation`() = runTest {
        purchaseRepository.create(aPurchase(id = "orphan", status = PurchaseStatus.AwaitingPayment))
        val viewModel = purchasesViewModel()

        viewModel.uiState.test {
            advanceUntilIdle()
            assertThat(expectMostRecentItem().canAbandon).isFalse()

            viewModel.onAction(PurchasesAction.CheckPayment)
            advanceUntilIdle()
            reconcile.onOrdersFailed()
            advanceUntilIdle()
            assertThat(expectMostRecentItem().canAbandon).isTrue()

            viewModel.onAction(PurchasesAction.AskToAbandon)
            advanceUntilIdle()
            assertThat(expectMostRecentItem().confirmingAbandon).isTrue()

            viewModel.onAction(PurchasesAction.ConfirmAbandon)
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertThat(state.confirmingAbandon).isFalse()
            // Abandonada, sem dinheiro envolvido: sai de "Minhas compras".
            assertThat(state.purchases).isEmpty()
        }
        assertThat(purchaseRepository.find("orphan")!!.status).isEqualTo(PurchaseStatus.Failed(FailureReason.Abandoned))
    }

    @Test
    fun `asking to give up before an inconclusive check opens nothing`() = runTest {
        purchaseRepository.create(aPurchase(id = "orphan", status = PurchaseStatus.AwaitingPayment))
        val viewModel = purchasesViewModel()

        viewModel.uiState.test {
            viewModel.onAction(PurchasesAction.AskToAbandon)
            advanceUntilIdle()

            assertThat(expectMostRecentItem().confirmingAbandon).isFalse()
        }
    }

    @Test
    fun `purchases flagged for refund carry the data to refund them, and the others carry none`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "event-1", capacity = 10)))
        val paid = aCieloOrder(authorizedAmount = aPurchase(id = "any").total, orderId = "kept")
        purchaseRepository.create(aPurchase(id = "duplicate", status = PurchaseStatus.AwaitingPayment, createdAtEpochMillis = 4_000))
        purchaseRepository.approve("duplicate", paid, extraOrderIds = listOf("extra"))
        val half = aCieloOrder(authorizedAmount = Money(cents = 2_500), orderId = "half")
        purchaseRepository.create(aPurchase(id = "mismatch", status = PurchaseStatus.AwaitingPayment, createdAtEpochMillis = 3_000))
        purchaseRepository.fail("mismatch", FailureReason.AmountMismatch, half, pendingRefund = true)
        purchaseRepository.create(aPurchase(id = "sold-out", status = PurchaseStatus.AwaitingPayment, createdAtEpochMillis = 2_000))
        purchaseRepository.fail("sold-out", FailureReason.SoldOutAfterPayment, paid, pendingRefund = true)
        purchaseRepository.create(aPurchase(id = "approved", createdAtEpochMillis = 1_000))

        purchasesViewModel().uiState.test {
            skipItems(1)

            val refunds = awaitItem().pendingRefunds
            assertThat(refunds).containsExactly(
                "duplicate", PendingRefundDetails(reason = null, refundedOrder = null, extraOrderIds = listOf("extra")),
                "mismatch", PendingRefundDetails(FailureReason.AmountMismatch, half, extraOrderIds = emptyList()),
                "sold-out", PendingRefundDetails(FailureReason.SoldOutAfterPayment, paid, extraOrderIds = emptyList()),
            )
        }
    }

    @Test
    fun `a charge that sold out after payment shows a blocking dialog until understood, and keeps the badge`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "event-1", capacity = 0)))
        purchaseRepository.create(aPurchase(id = "late", status = PurchaseStatus.Failed(FailureReason.NotCompleted)))
        val viewModel = purchasesViewModel()

        viewModel.uiState.test {
            skipItems(1)
            assertThat(awaitItem().soldOutNotice).isNull()

            HandlePaymentOutcomeUseCase(purchaseRepository)(
                PaymentOutcome.Paid(reference = "late", cieloOrder = aCieloOrder(authorizedAmount = aPurchase(id = "late").total)),
            )
            advanceUntilIdle()
            assertThat(expectMostRecentItem().soldOutNotice?.id).isEqualTo("late")

            viewModel.onAction(PurchasesAction.AcknowledgeSoldOut("late"))
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertThat(state.soldOutNotice).isNull()
            assertThat(state.pendingRefunds.keys).containsExactly("late")
        }
    }

    private fun TestScope.purchasesViewModel(): PurchasesViewModel {
        reconcile = ReconcilePurchaseUseCase(purchaseRepository, terminal, scope = backgroundScope)
        return PurchasesViewModel(
            observePurchases = ObservePurchasesUseCase(purchaseRepository),
            reconcilePurchase = reconcile,
            abandonPurchase = AbandonPurchaseUseCase(purchaseRepository, reconcile),
            observeSoldOutNotices = ObserveSoldOutNoticesUseCase(purchaseRepository),
            acknowledgeSoldOut = AcknowledgeSoldOutUseCase(purchaseRepository),
            savedStateHandle = SavedStateHandle(),
        )
    }
}
