package dev.railson.ticketsales.core.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dev.railson.ticketsales.core.data.local.database.TicketSalesDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Banco em arquivo: o Cart tem que sobreviver ao fechamento do app, então o teste fecha e reabre o banco. */
@RunWith(RobolectricTestRunner::class)
class RoomCartRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    @After
    fun deleteDatabase() {
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun `the cart survives closing and reopening the database`() = runTest {
        val firstOpening = TicketSalesDatabase.build(context, DATABASE_NAME)
        val (show, play) = firstOpening.eventDao().observeAll().first().map { it.id }
        RoomCartRepository(firstOpening.cartItemDao()).run {
            setQuantity(show, 2)
            setQuantity(play, 1)
        }
        firstOpening.close()

        val secondOpening = TicketSalesDatabase.build(context, DATABASE_NAME)
        val quantities = RoomCartRepository(secondOpening.cartItemDao()).observeQuantities().first()

        assertThat(quantities).containsExactly(show, 2, play, 1).inOrder()
        secondOpening.close()
    }

    @Test
    fun `changing a quantity keeps the item in its place and removing takes it out`() = runTest {
        val database = TicketSalesDatabase.build(context, DATABASE_NAME)
        val (show, play, talk) = database.eventDao().observeAll().first().map { it.id }
        val repository = RoomCartRepository(database.cartItemDao())
        repository.setQuantity(show, 1)
        repository.setQuantity(play, 1)
        repository.setQuantity(talk, 1)

        repository.setQuantity(show, 3)
        repository.remove(play)

        assertThat(repository.observeQuantities().first()).containsExactly(show, 3, talk, 1).inOrder()
        database.close()
    }

    private companion object {
        const val DATABASE_NAME = "cart-test.db"
    }
}
