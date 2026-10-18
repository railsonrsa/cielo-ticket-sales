package dev.railson.ticketsales.core.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dev.railson.ticketsales.core.data.local.database.TicketSalesDatabase
import dev.railson.ticketsales.core.domain.model.CieloOrder
import dev.railson.ticketsales.core.domain.model.FailureReason
import dev.railson.ticketsales.core.domain.model.Money
import dev.railson.ticketsales.core.domain.model.Purchase
import dev.railson.ticketsales.core.domain.model.PurchaseItem
import dev.railson.ticketsales.core.domain.model.PurchaseStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** A aprovação atômica contra SQL de verdade, sobre os Events do seed. */
@RunWith(RobolectricTestRunner::class)
class RoomPurchaseRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: TicketSalesDatabase
    private lateinit var repository: RoomPurchaseRepository
    private lateinit var cartRepository: RoomCartRepository

    @Before
    fun openDatabase() {
        context.deleteDatabase(DATABASE_NAME)
        database = TicketSalesDatabase.build(context, DATABASE_NAME)
        repository = RoomPurchaseRepository(database)
        cartRepository = RoomCartRepository(database.cartItemDao())
    }

    @After
    fun closeDatabase() {
        database.close()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun `approving issues one ticket per unit, takes the capacity and empties the cart in one go`() = runTest {
        val (show, play) = seededEvents()
        cartRepository.setQuantity(show.id, 2)
        cartRepository.setQuantity(play.id, 1)
        repository.create(awaitingPurchase(PurchaseItem(show.id, show.name, 2, Money(4_000)), PurchaseItem(play.id, play.name, 1, Money(2_500))))

        repository.approve(PURCHASE_ID, CIELO_ORDER)

        val approved = repository.find(PURCHASE_ID)!!
        assertThat(approved.status).isEqualTo(PurchaseStatus.Approved)
        assertThat(approved.cieloOrder).isEqualTo(CIELO_ORDER)
        val tickets = repository.observeTickets(PURCHASE_ID).first()
        assertThat(tickets.map { it.eventId }).containsExactly(show.id, show.id, play.id)
        assertThat(tickets.map { it.id }.toSet()).hasSize(3)
        assertThat(capacityOf(show.id)).isEqualTo(show.capacity - 2)
        assertThat(capacityOf(play.id)).isEqualTo(play.capacity - 1)
        assertThat(cartRepository.observeQuantities().first()).isEmpty()
    }

    @Test
    fun `an approval that does not fit the capacity changes nothing`() = runTest {
        val (show, play) = seededEvents()
        cartRepository.setQuantity(show.id, 2)
        cartRepository.setQuantity(play.id, 1)
        repository.create(awaitingPurchase(PurchaseItem(show.id, show.name, 2, Money(4_000)), PurchaseItem(play.id, play.name, 1, Money(2_500))))
        database.openHelper.writableDatabase.execSQL("UPDATE event SET capacity = 0 WHERE id = ?", arrayOf(play.id))

        assertThrows(IllegalStateException::class.java) {
            runBlocking { repository.approve(PURCHASE_ID, CIELO_ORDER) }
        }

        assertThat(repository.find(PURCHASE_ID)!!.status).isEqualTo(PurchaseStatus.AwaitingPayment)
        assertThat(repository.observeTickets(PURCHASE_ID).first()).isEmpty()
        assertThat(capacityOf(show.id)).isEqualTo(show.capacity)
        assertThat(cartRepository.observeQuantities().first()).containsExactly(show.id, 2, play.id, 1).inOrder()
    }

    @Test
    fun `a purchase keeps its frozen items, failure reason and refund flag`() = runTest {
        val (show, play) = seededEvents()
        val items = listOf(PurchaseItem(play.id, "Nome antigo", 1, Money(2_500)), PurchaseItem(show.id, show.name, 3, Money(4_000)))
        repository.create(awaitingPurchase(*items.toTypedArray()))
        assertThat(repository.findAwaitingPayment()!!.items).isEqualTo(items)

        repository.fail(PURCHASE_ID, FailureReason.AmountMismatch, CIELO_ORDER, pendingRefund = true)

        val failed = repository.find(PURCHASE_ID)!!
        assertThat(failed.status).isEqualTo(PurchaseStatus.Failed(FailureReason.AmountMismatch))
        assertThat(failed.cieloOrder).isEqualTo(CIELO_ORDER)
        assertThat(failed.pendingRefund).isTrue()
        assertThat(failed.items).isEqualTo(items)
        assertThat(repository.findAwaitingPayment()).isNull()
    }

    @Test
    fun `observing all purchases follows every change on disk`() = runTest {
        val (show) = seededEvents()
        repository.create(awaitingPurchase(PurchaseItem(show.id, show.name, 1, Money(4_000))))
        assertThat(repository.observeAll().first().single().status).isEqualTo(PurchaseStatus.AwaitingPayment)

        repository.approve(PURCHASE_ID, CIELO_ORDER)

        val all = repository.observeAll().first()
        assertThat(all.single().status).isEqualTo(PurchaseStatus.Approved)
        assertThat(all.single().items.single().eventId).isEqualTo(show.id)
    }

    @Test
    fun `approving with extra paid orders keeps them flagged for refund`() = runTest {
        val (show) = seededEvents()
        repository.create(awaitingPurchase(PurchaseItem(show.id, show.name, 1, Money(4_000))))

        repository.approve(PURCHASE_ID, CIELO_ORDER, extraOrderIds = listOf("order-2", "order-3"))

        val approved = repository.find(PURCHASE_ID)!!
        assertThat(approved.status).isEqualTo(PurchaseStatus.Approved)
        assertThat(approved.pendingRefund).isTrue()
        assertThat(approved.extraOrderIds).containsExactly("order-2", "order-3").inOrder()
    }

    @Test
    fun `an inconclusive check only bumps the count of the orphan`() = runTest {
        val (show) = seededEvents()
        repository.create(awaitingPurchase(PurchaseItem(show.id, show.name, 1, Money(4_000))))
        assertThat(repository.find(PURCHASE_ID)!!.extraOrderIds).isEmpty()

        repository.recordInconclusiveCheck(PURCHASE_ID)
        repository.recordInconclusiveCheck(PURCHASE_ID)

        val orphan = repository.findAwaitingPayment()!!
        assertThat(orphan.inconclusiveChecks).isEqualTo(2)
        assertThat(orphan.canBeAbandoned).isTrue()
    }

    private suspend fun seededEvents() = database.eventDao().observeAll().first().filter { it.capacity >= 10 }

    private suspend fun capacityOf(eventId: String) = database.eventDao().observeAll().first().single { it.id == eventId }.capacity

    private fun awaitingPurchase(vararg items: PurchaseItem) = Purchase(
        id = PURCHASE_ID,
        status = PurchaseStatus.AwaitingPayment,
        items = items.toList(),
        total = Money(items.sumOf { it.unitPrice.cents * it.quantity }),
        createdAtEpochMillis = 1_792_000_000_000,
    )

    private companion object {
        const val DATABASE_NAME = "purchase-test.db"
        const val PURCHASE_ID = "purchase-1"
        val CIELO_ORDER = CieloOrder(orderId = "order-1", authorizedAmount = Money(10_500), cieloCode = "123456", authCode = "654321")
    }
}
