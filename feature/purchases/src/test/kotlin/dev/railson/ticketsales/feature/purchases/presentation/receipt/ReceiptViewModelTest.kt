package dev.railson.ticketsales.feature.purchases.presentation.receipt

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.railson.ticketsales.core.domain.model.Money
import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.model.PurchaseItem
import dev.railson.ticketsales.core.domain.model.PurchaseStatus
import dev.railson.ticketsales.core.domain.model.aCieloOrder
import dev.railson.ticketsales.core.domain.model.anEvent
import dev.railson.ticketsales.core.domain.repository.FakeCartRepository
import dev.railson.ticketsales.core.domain.repository.FakeEventRepository
import dev.railson.ticketsales.core.domain.repository.FakePurchaseRepository
import dev.railson.ticketsales.core.domain.usecase.purchase.ObservePurchaseUseCase
import dev.railson.ticketsales.core.domain.usecase.purchase.ObserveTicketsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptViewModelTest {

    private val eventRepository = FakeEventRepository()
    private val purchaseRepository = FakePurchaseRepository(eventRepository, FakeCartRepository())

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `shows the approved purchase with one ticket per unit, named after the frozen item`() = runTest {
        eventRepository.setEvents(listOf(anEvent(id = "show", name = "Nome novo"), anEvent(id = "play")))
        val cieloOrder = aCieloOrder(authorizedAmount = Money(cents = 10_500))
        purchaseRepository.create(
            Purchase(
                id = "purchase-1",
                status = PurchaseStatus.AwaitingPayment,
                items = listOf(PurchaseItem("show", "Show", 2, Money(cents = 4_000)), PurchaseItem("play", "Peça", 1, Money(cents = 2_500))),
                total = Money(cents = 10_500),
                createdAtEpochMillis = 0,
            ),
        )
        purchaseRepository.approve("purchase-1", cieloOrder)

        receiptViewModel("purchase-1").uiState.test {
            assertThat(awaitItem().purchase).isNull()

            val shown = awaitItem()
            assertThat(shown.purchase!!.cieloOrder).isEqualTo(cieloOrder)
            assertThat(shown.tickets.map { it.eventId }).containsExactly("show", "show", "play").inOrder()
            assertThat(shown.tickets.map { it.id }.toSet()).hasSize(3)
            assertThat(shown.tickets.map(shown::eventName)).containsExactly("Show", "Show", "Peça").inOrder()
        }
    }

    private fun receiptViewModel(purchaseId: String) = ReceiptViewModel(
        purchaseId = purchaseId,
        observePurchase = ObservePurchaseUseCase(purchaseRepository),
        observeTickets = ObserveTicketsUseCase(purchaseRepository),
    )
}
