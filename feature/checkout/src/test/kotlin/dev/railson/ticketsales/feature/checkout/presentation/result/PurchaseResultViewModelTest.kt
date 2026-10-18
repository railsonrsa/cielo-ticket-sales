package dev.railson.ticketsales.feature.checkout.presentation.result

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.railson.ticketsales.core.domain.model.Money
import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.model.PurchaseItem
import dev.railson.ticketsales.core.domain.model.PurchaseStatus
import dev.railson.ticketsales.core.domain.repository.FakeCartRepository
import dev.railson.ticketsales.core.domain.repository.FakeEventRepository
import dev.railson.ticketsales.core.domain.repository.FakePurchaseRepository
import dev.railson.ticketsales.core.domain.usecase.purchase.ObservePurchaseUseCase
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
class PurchaseResultViewModelTest {

    private val purchaseRepository = FakePurchaseRepository(FakeEventRepository(), FakeCartRepository())

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `shows the purchase read from disk`() = runTest {
        val purchase = Purchase(
            id = "purchase-1",
            status = PurchaseStatus.Approved,
            items = listOf(PurchaseItem("show", "Show", quantity = 3, unitPrice = Money(cents = 4_000))),
            total = Money(cents = 12_000),
            createdAtEpochMillis = 0,
        )
        purchaseRepository.create(purchase)

        PurchaseResultViewModel("purchase-1", ObservePurchaseUseCase(purchaseRepository)).uiState.test {
            assertThat(awaitItem().purchase).isNull()
            val shown = awaitItem().purchase!!
            assertThat(shown).isEqualTo(purchase)
            assertThat(shown.ticketCount).isEqualTo(3)
        }
    }
}
