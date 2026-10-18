package dev.railson.ticketsales.payment.cielolio.callback

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dev.railson.ticketsales.core.data.di.dataModule
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.model.Money
import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.model.PurchaseItem
import dev.railson.ticketsales.core.domain.model.PurchaseStatus
import dev.railson.ticketsales.core.domain.repository.PurchaseRepository
import dev.railson.ticketsales.payment.cielolio.di.cieloLioModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.java.KoinJavaComponent.get
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** O `order://response` de ponta a ponta: do intent da Cielo até o banco, com o Room e o seed de verdade. */
@RunWith(RobolectricTestRunner::class)
class LioCallbackActivityTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var purchaseRepository: PurchaseRepository

    @Before
    fun startApp() {
        context.deleteDatabase(DATABASE_NAME)
        startKoin {
            androidContext(context)
            modules(dataModule, cieloLioModule)
        }
        purchaseRepository = get(PurchaseRepository::class.java)
    }

    @After
    fun stopApp() {
        stopKoin()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun `an approved callback approves the purchase awaiting payment and issues its ticket`() {
        // O reference e o valor da fixture C1.
        val purchase = awaitingPurchase(id = "6e46c58a-ca07-43be-9242-d12aee9d96fd", total = Money(5_000))
        runBlocking { purchaseRepository.create(purchase) }

        val activity = deliverCallback(fixtureBase64("payment-approved.json"))

        val stored = awaitOutcome(purchase.id)
        assertThat(stored.status).isEqualTo(PurchaseStatus.Approved)
        assertThat(stored.cieloOrder?.orderId).isEqualTo("cbd76f56-d782-4cf9-8db8-f76615be44d7")
        assertThat(runBlocking { purchaseRepository.observeTickets(purchase.id).first() }).hasSize(1)
        awaitMainLooper { activity.isFinishing }
    }

    @Test
    fun `a canceled callback without reference fails the only purchase awaiting payment`() {
        val purchase = awaitingPurchase(id = "purchase-1", total = Money(5_000))
        runBlocking { purchaseRepository.create(purchase) }

        deliverCallback(fixtureBase64("failure-canceled.json"))

        assertThat(awaitOutcome(purchase.id).status).isEqualTo(PurchaseStatus.Failed(FailureReason.CanceledByOperator))
    }

    private fun deliverCallback(response: String): LioCallbackActivity {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("order://response?response=$response&responsecode=0"))
            .setClass(context, LioCallbackActivity::class.java)
        return Robolectric.buildActivity(LioCallbackActivity::class.java, intent).setup().get()
    }

    /** O use case grava em outra thread: espera a Purchase sair de `AwaitingPayment`. */
    private fun awaitOutcome(purchaseId: String): Purchase {
        awaitMainLooper { runBlocking { purchaseRepository.find(purchaseId) }!!.status != PurchaseStatus.AwaitingPayment }
        return runBlocking { purchaseRepository.find(purchaseId) }!!
    }

    private fun awaitMainLooper(condition: () -> Boolean) {
        repeat(200) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(10)
        }
        error("a condição não se cumpriu")
    }

    private fun awaitingPurchase(id: String, total: Money) = Purchase(
        id = id,
        status = PurchaseStatus.AwaitingPayment,
        items = listOf(PurchaseItem(SEEDED_EVENT_ID, "Noite de Choro no Coreto", quantity = 1, unitPrice = total)),
        total = total,
        createdAtEpochMillis = 0,
    )

    private companion object {
        const val DATABASE_NAME = "ticket-sales.db"
        const val SEEDED_EVENT_ID = "7e8e3497-5e2a-420d-9826-9bb0ebf57f9c"
    }
}
