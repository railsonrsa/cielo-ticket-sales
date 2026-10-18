package dev.railson.ticketsales.feature.purchases.presentation.purchases

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.model.PurchaseStatus
import dev.railson.ticketsales.core.domain.model.aCieloOrder
import dev.railson.ticketsales.core.domain.model.aPurchase
import dev.railson.ticketsales.core.domain.model.anEvent
import dev.railson.ticketsales.core.domain.repository.FakeCartRepository
import dev.railson.ticketsales.core.domain.repository.FakeEventRepository
import dev.railson.ticketsales.core.domain.repository.FakePurchaseRepository
import dev.railson.ticketsales.core.domain.usecase.purchase.ObservePurchasesUseCase
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
class PurchasesViewModelTest {

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
    fun `starts loading and then shows the purchases in the order of the list`() = runTest {
        purchaseRepository.create(aPurchase(id = "approved", createdAtEpochMillis = 2_000))
        purchaseRepository.create(aPurchase(id = "declined", status = PurchaseStatus.Failed(FailureReason.Declined)))
        purchaseRepository.create(aPurchase(id = "awaiting", status = PurchaseStatus.AwaitingPayment, createdAtEpochMillis = 1_000))

        PurchasesViewModel(ObservePurchasesUseCase(purchaseRepository)).uiState.test {
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

        PurchasesViewModel(ObservePurchasesUseCase(purchaseRepository)).uiState.test {
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
        PurchasesViewModel(ObservePurchasesUseCase(purchaseRepository)).uiState.test {
            skipItems(1)
            assertThat(awaitItem()).isEqualTo(PurchasesUiState(isLoading = false, purchases = emptyList()))
        }
    }
}
