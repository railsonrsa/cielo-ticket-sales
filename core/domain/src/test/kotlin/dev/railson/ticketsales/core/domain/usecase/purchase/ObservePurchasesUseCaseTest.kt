package dev.railson.ticketsales.core.domain.usecase.purchase

import com.google.common.truth.Truth.assertThat
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.model.PurchaseStatus
import dev.railson.ticketsales.core.domain.model.aPurchase
import dev.railson.ticketsales.core.domain.repository.FakeCartRepository
import dev.railson.ticketsales.core.domain.repository.FakeEventRepository
import dev.railson.ticketsales.core.domain.repository.FakePurchaseRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ObservePurchasesUseCaseTest {

    private val purchaseRepository = FakePurchaseRepository(FakeEventRepository(), FakeCartRepository())
    private val observePurchases = ObservePurchasesUseCase(purchaseRepository)

    @Test
    fun `approved purchases come from the newest to the oldest`() = runTest {
        purchaseRepository.create(aPurchase(id = "monday", createdAtEpochMillis = 1_000))
        purchaseRepository.create(aPurchase(id = "wednesday", createdAtEpochMillis = 3_000))
        purchaseRepository.create(aPurchase(id = "tuesday", createdAtEpochMillis = 2_000))

        val shown = observePurchases().first()

        assertThat(shown.map { it.id }).containsExactly("wednesday", "tuesday", "monday").inOrder()
    }

    @Test
    fun `failed purchases without money involved stay out, the ones pending refund stay in`() = runTest {
        purchaseRepository.create(aPurchase(id = "declined", status = PurchaseStatus.Failed(FailureReason.Declined)))
        purchaseRepository.create(aPurchase(id = "abandoned", status = PurchaseStatus.Failed(FailureReason.Abandoned)))
        purchaseRepository.create(
            aPurchase(id = "sold-out", status = PurchaseStatus.Failed(FailureReason.SoldOutAfterPayment), pendingRefund = true),
        )

        val shown = observePurchases().first()

        assertThat(shown.map { it.id }).containsExactly("sold-out")
    }

    @Test
    fun `the purchase awaiting payment goes on top, then the ones pending refund, then the approved ones`() = runTest {
        purchaseRepository.create(aPurchase(id = "approved-old", createdAtEpochMillis = 1_000))
        purchaseRepository.create(aPurchase(id = "awaiting", status = PurchaseStatus.AwaitingPayment, createdAtEpochMillis = 2_000))
        purchaseRepository.create(aPurchase(id = "approved-new", createdAtEpochMillis = 6_000))
        purchaseRepository.create(aPurchase(id = "charged-twice", createdAtEpochMillis = 3_000, pendingRefund = true))
        purchaseRepository.create(
            aPurchase(
                id = "amount-mismatch",
                status = PurchaseStatus.Failed(FailureReason.AmountMismatch),
                createdAtEpochMillis = 5_000,
                pendingRefund = true,
            ),
        )

        val shown = observePurchases().first()

        assertThat(shown.map { it.id })
            .containsExactly("awaiting", "amount-mismatch", "charged-twice", "approved-new", "approved-old")
            .inOrder()
    }
}
