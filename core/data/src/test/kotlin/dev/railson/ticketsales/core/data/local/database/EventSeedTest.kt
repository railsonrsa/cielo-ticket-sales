package dev.railson.ticketsales.core.data.local.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Banco em arquivo (não em memória) porque o teste precisa fechar e reabrir o mesmo banco. */
@RunWith(RobolectricTestRunner::class)
class EventSeedTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    @After
    fun deleteDatabase() {
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun `the seed fills the event table when the database is created`() = runTest {
        val database = TicketSalesDatabase.build(context, DATABASE_NAME)

        val events = database.eventDao().observeAll().first()

        assertThat(events).hasSize(6)
        assertThat(events.map { it.capacity }).contains(2)
        database.close()
    }

    @Test
    fun `reopening the database does not reload the seed over sold capacity`() = runTest {
        val firstOpening = TicketSalesDatabase.build(context, DATABASE_NAME)
        val event = firstOpening.eventDao().observeAll().first().first()
        firstOpening.openHelper.writableDatabase.execSQL(
            "UPDATE event SET capacity = capacity - 1 WHERE id = ?",
            arrayOf(event.id),
        )
        firstOpening.close()

        val secondOpening = TicketSalesDatabase.build(context, DATABASE_NAME)
        val events = secondOpening.eventDao().observeAll().first()

        assertThat(events).hasSize(6)
        assertThat(events.single { it.id == event.id }.capacity).isEqualTo(event.capacity - 1)
        secondOpening.close()
    }

    private companion object {
        const val DATABASE_NAME = "event-seed-test.db"
    }
}
