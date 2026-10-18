package dev.railson.ticketsales.core.domain.usecase.payment

import com.google.common.truth.Truth.assertThat
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.model.PurchaseStatus
import dev.railson.ticketsales.core.domain.model.aPurchase
import dev.railson.ticketsales.core.domain.repository.FakeCartRepository
import dev.railson.ticketsales.core.domain.repository.FakeEventRepository
import dev.railson.ticketsales.core.domain.repository.FakePurchaseRepository
import dev.railson.ticketsales.core.domain.terminal.RecordingPaymentTerminal
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AbandonPurchaseUseCaseTest {

    private val cartRepository = FakeCartRepository()
    private val purchaseRepository = FakePurchaseRepository(FakeEventRepository(), cartRepository)
    private val terminal = RecordingPaymentTerminal(purchaseRepository)

    private fun TestScope.newUseCases(): Pair<ReconcilePurchaseUseCase, AbandonPurchaseUseCase> {
        val reconcile = ReconcilePurchaseUseCase(purchaseRepository, terminal, scope = backgroundScope)
        return reconcile to AbandonPurchaseUseCase(purchaseRepository, reconcile)
    }

    private suspend fun orphan() = purchaseRepository.find(ORPHAN)!!

    @Test
    fun `giving up is refused before any inconclusive check`() = runTest {
        purchaseRepository.create(aPurchase(id = ORPHAN, status = PurchaseStatus.AwaitingPayment))
        val (_, abandon) = newUseCases()

        assertThat(abandon(ORPHAN)).isFalse()
        assertThat(orphan().status).isEqualTo(PurchaseStatus.AwaitingPayment)
    }

    @Test
    fun `giving up after an inconclusive check fails the orphan as abandoned`() = runTest {
        purchaseRepository.create(aPurchase(id = ORPHAN, status = PurchaseStatus.AwaitingPayment))
        val (reconcile, abandon) = newUseCases()
        reconcile()
        reconcile.onOrdersFailed()

        assertThat(abandon(ORPHAN)).isTrue()
        assertThat(orphan().status).isEqualTo(PurchaseStatus.Failed(FailureReason.Abandoned))
        assertThat(orphan().pendingRefund).isFalse()
    }

    @Test
    fun `giving up is refused while a check is running`() = runTest {
        purchaseRepository.create(aPurchase(id = ORPHAN, status = PurchaseStatus.AwaitingPayment, inconclusiveChecks = 1))
        val (reconcile, abandon) = newUseCases()
        reconcile()

        assertThat(abandon(ORPHAN)).isFalse()
        assertThat(orphan().status).isEqualTo(PurchaseStatus.AwaitingPayment)
    }

    @Test
    fun `giving up does not touch a purchase that already has an outcome`() = runTest {
        purchaseRepository.create(aPurchase(id = ORPHAN, status = PurchaseStatus.Approved, inconclusiveChecks = 1))
        val (_, abandon) = newUseCases()

        assertThat(abandon(ORPHAN)).isFalse()
        assertThat(orphan().status).isEqualTo(PurchaseStatus.Approved)
    }

    private companion object {
        const val ORPHAN = "orphan"
    }
}
