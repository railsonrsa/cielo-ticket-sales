package dev.railson.ticketsales.feature.checkout.presentation.cart

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.railson.ticketsales.core.domain.model.CapacityConflict
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.model.Money
import dev.railson.ticketsales.core.domain.model.aCieloOrder
import dev.railson.ticketsales.core.domain.model.anEvent
import dev.railson.ticketsales.core.domain.repository.FakeCartRepository
import dev.railson.ticketsales.core.domain.repository.FakeEventRepository
import dev.railson.ticketsales.core.domain.repository.FakePurchaseRepository
import dev.railson.ticketsales.core.domain.terminal.PaymentOutcome
import dev.railson.ticketsales.core.domain.terminal.RecordingPaymentTerminal
import dev.railson.ticketsales.core.domain.usecase.cart.ChangeCartQuantityUseCase
import dev.railson.ticketsales.core.domain.usecase.cart.ObserveCartUseCase
import dev.railson.ticketsales.core.domain.usecase.cart.RemoveFromCartUseCase
import dev.railson.ticketsales.core.domain.usecase.payment.HandlePaymentOutcomeUseCase
import dev.railson.ticketsales.core.domain.usecase.payment.StartPurchaseUseCase
import dev.railson.ticketsales.core.domain.usecase.purchase.ObserveAwaitingPaymentUseCase
import dev.railson.ticketsales.core.domain.usecase.purchase.ObservePurchaseUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelTest {

    private val eventRepository = FakeEventRepository()
    private val cartRepository = FakeCartRepository()
    private val purchaseRepository = FakePurchaseRepository(eventRepository, cartRepository)
    private val terminal = RecordingPaymentTerminal(purchaseRepository)
    private val handleOutcome = HandlePaymentOutcomeUseCase(purchaseRepository)

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `shows the cart with the total at current prices and lets the operator pay`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "show", unitPrice = Money(cents = 4_000), capacity = 10)))
        cartRepository.setQuantity("show", 3)
        val viewModel = cartViewModel()

        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(CartUiState(isLoading = true))

            val loaded = awaitItem()
            assertThat(loaded.cart.total).isEqualTo(Money(cents = 12_000))
            assertThat(loaded.canPay).isTrue()
        }
    }

    @Test
    fun `an empty cart cannot be paid`() = runTest {
        val viewModel = cartViewModel()

        viewModel.uiState.test {
            skipItems(1)
            assertThat(awaitItem().canPay).isFalse()
        }
    }

    @Test
    fun `a capacity drop flags the item and blocks paying until it is adjusted`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "show", capacity = 5)))
        cartRepository.setQuantity("show", 4)
        val viewModel = cartViewModel()

        viewModel.uiState.test {
            skipItems(1)
            assertThat(awaitItem().canPay).isTrue()

            eventRepository.setEvents(listOf(anEvent(id = "show", capacity = 2)))

            val inConflict = awaitItem()
            assertThat(inConflict.cart.items.single().capacityConflict).isEqualTo(CapacityConflict.OnlyLeft(available = 2))
            assertThat(inConflict.canPay).isFalse()

            viewModel.onAction(CartAction.ChangeQuantity("show", 2))

            val adjusted = awaitItem()
            assertThat(adjusted.cart.items.single().quantity).isEqualTo(2)
            assertThat(adjusted.cart.items.single().capacityConflict).isNull()
            assertThat(adjusted.canPay).isTrue()
        }
    }

    @Test
    fun `removing a sold out item unblocks paying for the rest`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "show", capacity = 0), anEvent(id = "play", capacity = 10)))
        cartRepository.setQuantity("show", 1)
        cartRepository.setQuantity("play", 2)
        val viewModel = cartViewModel()

        viewModel.uiState.test {
            skipItems(1)
            val inConflict = awaitItem()
            assertThat(inConflict.cart.items.first().capacityConflict).isEqualTo(CapacityConflict.SoldOut)
            assertThat(inConflict.canPay).isFalse()

            viewModel.onAction(CartAction.Remove("show"))
            advanceUntilIdle()

            val afterRemoval = expectMostRecentItem()
            assertThat(afterRemoval.cart.items.map { it.event.id }).containsExactly("play")
            assertThat(afterRemoval.canPay).isTrue()
        }
    }

    @Test
    fun `an approved payment navigates to the result`() = runTest {
        val viewModel = cartViewModelWithOneTicket()

        viewModel.effects.test {
            viewModel.onAction(CartAction.Pay)
            advanceUntilIdle()
            val purchase = terminal.requested.single()

            handleOutcome(PaymentOutcome.Paid(purchase.id, aCieloOrder(authorizedAmount = purchase.total)))

            assertThat(awaitItem()).isEqualTo(CartEffect.PurchaseApproved(purchase.id))
        }
    }

    @Test
    fun `a payment canceled on the terminal shows a snackbar and keeps the cart`() = runTest {
        val viewModel = cartViewModelWithOneTicket()

        viewModel.uiState.test {
            viewModel.effects.test {
                viewModel.onAction(CartAction.Pay)
                advanceUntilIdle()

                handleOutcome(PaymentOutcome.Failed(FailureReason.CanceledByOperator))

                assertThat(awaitItem()).isEqualTo(CartEffect.PaymentCanceled)
            }
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertThat(state.dialog).isNull()
            assertThat(state.cart.items).hasSize(1)
            assertThat(state.canPay).isTrue()
        }
    }

    @Test
    fun `a declined payment offers a retry that starts a new purchase from the same cart`() = runTest {
        val viewModel = cartViewModelWithOneTicket()

        viewModel.uiState.test {
            viewModel.onAction(CartAction.Pay)
            advanceUntilIdle()
            handleOutcome(PaymentOutcome.Failed(FailureReason.Declined))
            advanceUntilIdle()
            assertThat(expectMostRecentItem().dialog).isEqualTo(CartDialog.RetryablePaymentFailure(FailureReason.Declined))

            viewModel.onAction(CartAction.Retry)
            advanceUntilIdle()

            val retrying = expectMostRecentItem()
            assertThat(retrying.dialog).isNull()
            assertThat(retrying.awaitingPayment).isTrue()
            val (first, second) = terminal.requested
            assertThat(second.id).isNotEqualTo(first.id)
            assertThat(second.items).isEqualTo(first.items)
        }
    }

    @Test
    fun `a terminal without authentication shows a dialog with no retry until dismissed`() = runTest {
        val viewModel = cartViewModelWithOneTicket()

        viewModel.uiState.test {
            viewModel.onAction(CartAction.Pay)
            advanceUntilIdle()
            handleOutcome(PaymentOutcome.Failed(FailureReason.TerminalAuthError))
            advanceUntilIdle()
            assertThat(expectMostRecentItem().dialog).isEqualTo(CartDialog.TerminalAuthError)

            viewModel.onAction(CartAction.DismissDialog)
            advanceUntilIdle()

            assertThat(expectMostRecentItem().dialog).isNull()
            assertThat(terminal.requested).hasSize(1)
        }
    }

    @Test
    fun `a payment with no answer shows the banner and blocks new sales`() = runTest {
        val viewModel = cartViewModelWithOneTicket()

        viewModel.uiState.test {
            viewModel.onAction(CartAction.Pay)
            advanceUntilIdle()
            handleOutcome(PaymentOutcome.Unreadable)
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertThat(state.awaitingPayment).isTrue()
            assertThat(state.canPay).isFalse()
            assertThat(state.dialog).isNull()
        }
    }

    private suspend fun cartViewModelWithOneTicket(): CartViewModel {
        eventRepository.setEvents(listOf(anEvent(id = "show", unitPrice = Money(cents = 4_000), capacity = 10)))
        cartRepository.setQuantity("show", 1)
        return cartViewModel()
    }

    private fun cartViewModel() = CartViewModel(
        observeCart = ObserveCartUseCase(cartRepository, eventRepository),
        changeQuantity = ChangeCartQuantityUseCase(cartRepository, eventRepository),
        removeFromCart = RemoveFromCartUseCase(cartRepository),
        startPurchase = StartPurchaseUseCase(ObserveCartUseCase(cartRepository, eventRepository), purchaseRepository, terminal),
        observeAwaitingPayment = ObserveAwaitingPaymentUseCase(purchaseRepository),
        observePurchase = ObservePurchaseUseCase(purchaseRepository),
        savedStateHandle = SavedStateHandle(),
    )
}
