package dev.railson.ticketsales.core.domain.usecase.cart

import com.google.common.truth.Truth.assertThat
import dev.railson.ticketsales.core.domain.model.CapacityConflict
import dev.railson.ticketsales.core.domain.model.Money
import dev.railson.ticketsales.core.domain.model.anEvent
import dev.railson.ticketsales.core.domain.repository.FakeCartRepository
import dev.railson.ticketsales.core.domain.repository.FakeEventRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CartUseCasesTest {

    private val eventRepository = FakeEventRepository()
    private val cartRepository = FakeCartRepository()

    private val observeCart = ObserveCartUseCase(cartRepository, eventRepository)
    private val addToCart = AddToCartUseCase(cartRepository, eventRepository)
    private val changeQuantity = ChangeCartQuantityUseCase(cartRepository, eventRepository)
    private val removeFromCart = RemoveFromCartUseCase(cartRepository)

    @Test
    fun `adding an event puts one ticket of it in the cart`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "show", unitPrice = Money(cents = 4_000))))

        addToCart("show")

        val item = observeCart().first().items.single()
        assertThat(item.event.id).isEqualTo("show")
        assertThat(item.quantity).isEqualTo(1)
        assertThat(item.subtotal).isEqualTo(Money(cents = 4_000))
    }

    @Test
    fun `adding beyond the current capacity is refused and the cart keeps its quantity`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "show", capacity = 2)))
        addToCart("show")
        addToCart("show")

        val result = addToCart("show")

        assertThat(result).isEqualTo(CartChangeResult.ExceedsCapacity)
        assertThat(observeCart().first().items.single().quantity).isEqualTo(2)
    }

    @Test
    fun `adjusting an item down to the remaining capacity is applied`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "show", capacity = 5)))
        changeQuantity("show", 5)
        eventRepository.setEvents(listOf(anEvent(id = "show", capacity = 2)))

        val result = changeQuantity("show", 2)

        assertThat(result).isEqualTo(CartChangeResult.Applied)
        assertThat(observeCart().first().items.single().quantity).isEqualTo(2)
    }

    @Test
    fun `changing to more than the current capacity is refused`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "show", capacity = 3)))
        changeQuantity("show", 2)

        val result = changeQuantity("show", 4)

        assertThat(result).isEqualTo(CartChangeResult.ExceedsCapacity)
        assertThat(observeCart().first().items.single().quantity).isEqualTo(2)
    }

    @Test
    fun `removing an event takes it out of the cart and keeps the others`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "show"), anEvent(id = "play")))
        addToCart("show")
        addToCart("play")

        removeFromCart("show")

        assertThat(observeCart().first().items.map { it.event.id }).containsExactly("play")
    }

    @Test
    fun `the total follows the current price of each event`() = runTest {
        eventRepository.setEvents(
            listOf(anEvent(id = "show", unitPrice = Money(cents = 4_000)), anEvent(id = "play", unitPrice = Money(cents = 2_550))),
        )
        changeQuantity("show", 2)
        addToCart("play")

        eventRepository.setEvents(
            listOf(anEvent(id = "show", unitPrice = Money(cents = 5_000)), anEvent(id = "play", unitPrice = Money(cents = 2_550))),
        )

        assertThat(observeCart().first().total).isEqualTo(Money(cents = 12_550))
    }

    @Test
    fun `an item above the current capacity is in conflict with how many are left`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "show", capacity = 5)))
        changeQuantity("show", 4)

        eventRepository.setEvents(listOf(anEvent(id = "show", capacity = 2)))

        val cart = observeCart().first()
        assertThat(cart.items.single().capacityConflict).isEqualTo(CapacityConflict.OnlyLeft(available = 2))
        assertThat(cart.hasCapacityConflict).isTrue()
    }

    @Test
    fun `an item whose event sold out is in conflict as sold out`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "show", capacity = 5)))
        addToCart("show")

        eventRepository.setEvents(listOf(anEvent(id = "show", capacity = 0)))

        assertThat(observeCart().first().items.single().capacityConflict).isEqualTo(CapacityConflict.SoldOut)
    }

    @Test
    fun `items within the current capacity have no conflict`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "show", capacity = 2)))
        changeQuantity("show", 2)

        val cart = observeCart().first()
        assertThat(cart.items.single().capacityConflict).isNull()
        assertThat(cart.hasCapacityConflict).isFalse()
    }
}
