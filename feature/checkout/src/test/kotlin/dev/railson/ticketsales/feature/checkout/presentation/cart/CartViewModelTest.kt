package dev.railson.ticketsales.feature.checkout.presentation.cart

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.railson.ticketsales.core.domain.model.CapacityConflict
import dev.railson.ticketsales.core.domain.model.Money
import dev.railson.ticketsales.core.domain.model.anEvent
import dev.railson.ticketsales.core.domain.repository.FakeCartRepository
import dev.railson.ticketsales.core.domain.repository.FakeEventRepository
import dev.railson.ticketsales.core.domain.usecase.cart.ChangeCartQuantityUseCase
import dev.railson.ticketsales.core.domain.usecase.cart.ObserveCartUseCase
import dev.railson.ticketsales.core.domain.usecase.cart.RemoveFromCartUseCase
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

    private fun cartViewModel() = CartViewModel(
        observeCart = ObserveCartUseCase(cartRepository, eventRepository),
        changeQuantity = ChangeCartQuantityUseCase(cartRepository, eventRepository),
        removeFromCart = RemoveFromCartUseCase(cartRepository),
    )
}
